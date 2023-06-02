using System;
using System.Runtime.CompilerServices;
using System.Threading.Tasks.Dataflow;

namespace DwebBrowser.MicroService.Message;

/**
 * <summary>
 * IpcBodySender 本质上是对 ReadableStream 的再次封装。
 * 我们知道 ReadableStream 本质上是由 stream 与 controller 组成。二者分别代表着 reader 与 writer 两个角色。
 *
 * 而 IpcBodySender 则是将 controller 给一个 ipc 来做写入，将 stream 给另一个 ipc 来做接收。
 * 而关键点就在于这两个 ipc 很可能不是对等关系
 *
 * 因为 IpcBodySender 会被 IpcRequest/http4kRequest、IpcResponse/http4kResponse 对象转换的时候传递，
 * 中间被很多个 ipc 所持有过，而每一个持有过它的人都有可能是这个 stream 的读取者。
 *
 * 因此我们定义了两个集合，一个是 ipc 的 usableIpcBodyMap；一个是 ipcBodySender 这边的 usedIpcMap
 * </summary>
 */

public class IpcBodySender : IpcBody
{
    static readonly Debugger Console = new("IpcBodySender");
    public override object? Raw { get; }
    public Ipc SenderIpc { get; set; }

    public bool IsStream
    {
        get { return new Lazy<bool>(new Func<bool>(() => Raw is Stream), true).Value; }
    }

    public bool IsStreamClosed
    {
        get { return IsStream ? _isStreamClosed : true; }
    }

    private bool _isStreamOpenedValue = false;
    private bool _isStreamOpened
    {
        get { return _isStreamOpenedValue; }
        set
        {
            if (_isStreamOpenedValue != value)
            {
                _isStreamOpenedValue = value;
            }
        }
    }

    private bool _isStreamClosedValue = false;
    private bool _isStreamClosed
    {
        get { return _isStreamClosedValue; }
        set
        {
            if (_isStreamClosedValue != value)
            {
                _isStreamClosedValue = value;
            }
        }
    }

    /**
     * 控制信号
     */
    enum StreamStatusSignal
    {
        PULLING,
        PAUSED,
        ABORTED,
    }

    private BufferBlock<StreamStatusSignal> _streamStatusSignal = new BufferBlock<StreamStatusSignal>(new DataflowBlockOptions { BoundedCapacity = DataflowBlockOptions.Unbounded });

    public class IPC
    {
        /// <summary>
        /// 某个 IPC 它所能读取的 ipcBody
        /// </summary>
        public class UsableIpcBodyMapper
        {
            private Dictionary</*streamId*/string, IpcBodySender> _map { get; set; }

            public UsableIpcBodyMapper(Dictionary<string, IpcBodySender> map)
            {
                _map = map;
            }

            public bool Add(string streamId, IpcBodySender ipcBody)
            {
                if (_map.ContainsKey(streamId))
                {
                    return false;
                }

                _map.Add(streamId, ipcBody);
                return true;
            }

            public IpcBodySender? Get(string streamId) => _map[streamId];

            public async Task<IpcBodySender?> Remove(string streamId)
            {
                var ipcBodySender = Get(streamId);

                if (ipcBodySender is not null)
                {
                    _map.Remove(streamId);
                }

                if (_map.Count == 0)
                {
                    OnDetroy?.Emit();
                    OnDetroy = null;
                }

                return ipcBodySender;
            }

            public event Signal? OnDetroy;

        }

        private static ConditionalWeakTable<Ipc, UsableIpcBodyMapper> s_ipcUsableIpcBodyMap = new();

        /**
        * <summary>
        * ipc 将会使用 ipcBody
        * 那么只要这个 ipc 接收到 pull 指令，就意味着成为"使用者"，那么这个 ipcBody 都会开始读取数据出来发送
        * 在开始发送第一帧数据之前，其它 ipc 也可以通过 pull 指令来参与成为"使用者"
        * </summary>
        */
        public static void UsableByIpc(Ipc ipc, IpcBodySender ipcBody)
        {
            if (!ipcBody.IsStream || ipcBody._isStreamOpened)
            {
                return;
            }

            var streamId = ipcBody.MetaBody.StreamId!;
            Console.Log("UsableByIpc", "ipc:{0}; streamId:{1}", ipc, streamId);

            var usableIpcBodyMapper = s_ipcUsableIpcBodyMap.GetValueOrPut(ipc, () =>
            {
                var mapper = new UsableIpcBodyMapper(new Dictionary<string, IpcBodySender>());

                Signal<IpcStream, Ipc> cb = async (ipcStream, ipc, self) =>
                {
                    switch (ipcStream)
                    {
                        case IpcStreamPulling ipcStreamPulling:
                            await (mapper.Get(ipcStreamPulling.StreamId)?._useByIpc(ipc)?.EmitStreamPull(ipcStreamPulling)).ForAwait();
                            break;
                        case IpcStreamPaused ipcStreamPaused:
                            await (mapper.Get(ipcStreamPaused.StreamId)?._useByIpc(ipc)?.EmitStreamPaused(ipcStreamPaused)).ForAwait();
                            break;
                        case IpcStreamAbort ipcStreamAbort:
                            await (mapper.Get(ipcStreamAbort.StreamId)?._useByIpc(ipc)?.EmitStreamAborted()).ForAwait();
                            break;
                        default:
                            break;
                    }

                };
                ipc.OnStream += cb;

                mapper.OnDetroy += async (_) => ipc.OnStream -= cb;
                mapper.OnDetroy += (_) => mapper.Remove(streamId);

                return mapper;
            });


            if (usableIpcBodyMapper.Add(streamId, ipcBody))
            {
                ipcBody.OnStreamClose += (_) =>
                {
                    return usableIpcBodyMapper.Remove(streamId);
                };
            }
        }
    }

    /// <summary>被哪些 ipc 所真正使用，以及它们对应的信息</summary>
    private Dictionary<Ipc, UsedIpcInfo> _usedIpcMap = new Dictionary<Ipc, UsedIpcInfo>();

    /// <summary>
    /// 绑定使用
    /// ipc 将使用这个 body，也就是说接下来的 MessageData 也要通知一份给这个 ipc
    /// 但一个流一旦开启了，那么就无法再被外部使用了
    /// </summary>
    internal class UsedIpcInfo
    {
        internal IpcBodySender UIpcBody { get; set; }
        internal Ipc Uipc { get; set; }
        internal int Bandwidth { get; set; }
        internal int Fuse { get; set; }

        internal UsedIpcInfo(IpcBodySender ipcBody, Ipc ipc, int bandwidth = 0, int fuse = 0)
        {
            UIpcBody = ipcBody;
            Uipc = ipc;
            Bandwidth = bandwidth;
            Fuse = fuse;
        }

        internal Task EmitStreamPull(IpcStreamPulling message) =>
            UIpcBody.EmitStreamPullAsync(this, message);

        internal Task EmitStreamPaused(IpcStreamPaused message) =>
            UIpcBody.EmitStreamPausedAsync(this, message);

        internal Task EmitStreamAborted() => UIpcBody.EmitStreamAbortedAsync(this);
    }

    private UsedIpcInfo? _useByIpc(Ipc ipc)
    {
        var usedIpcInfo = _usedIpcMap.GetValueOrDefault(ipc);

        if (usedIpcInfo is null)
        {
            if (IsStream)
            {
                if (!_isStreamOpened)
                {
                    return new UsedIpcInfo(this, ipc).Also(usedIpcInfo =>
                    {
                        _usedIpcMap[ipc] = usedIpcInfo;
                        OnStreamClose += (_) => EmitStreamAbortedAsync(usedIpcInfo);
                    });
                }
                else
                {
                    Console.Warn("useByIpc", "should not happend");
                }
            }
        }

        return usedIpcInfo;
    }

    /// <summary>
    /// 拉取数据
    /// </summary>
    private async Task EmitStreamPullAsync(UsedIpcInfo info, IpcStreamPulling message)
    {
        /// 更新带宽限制
        info.Bandwidth = message.Bandwidth;
        /// 只要有一个开始读取，那么就可以开始
        var success = await _streamStatusSignal.SendAsync(StreamStatusSignal.PULLING);

        Console.Log("EmitStreamPullAsync", "{0:H} {1}", _streamStatusSignal, success);
    }

    /// <summary>
    /// 暂停数据
    /// </summary>
    private async Task EmitStreamPausedAsync(UsedIpcInfo info, IpcStreamPaused message)
    {    /// 更新保险限制
        info.Bandwidth = -1;
        info.Fuse = message.Fuse;

        /// 如果所有的读取者都暂停了，那么就触发暂停
        var paused = true;
        foreach (UsedIpcInfo _info in _usedIpcMap.Values)
        {
            if (info.Bandwidth >= 0)
            {
                paused = false;
                break;
            }
        }
        if (paused)
        {
            await _streamStatusSignal.SendAsync(StreamStatusSignal.PAUSED);
        }
    }

    /// <summary>
    /// 解绑使用
    /// </summary>
    private async Task EmitStreamAbortedAsync(UsedIpcInfo info)
    {
        if (_usedIpcMap.Remove(info.Uipc))
        {
            if (_usedIpcMap.Count == 0)
            {
                await _streamStatusSignal.SendAsync(StreamStatusSignal.ABORTED);
            }
        }
    }

    public event Signal? OnStreamClose;

    public event Signal? OnStreamOpen;


    private void _emitStreamClose()
    {
        _isStreamOpened = true;
        _isStreamClosed = true;
    }

    protected override BodyHubType BodyHub
    {
        get
        {
            return new Lazy<BodyHubType>(new Func<BodyHubType>(() => new BodyHubType().Also(it =>
            {
                it.Data = Raw;
                switch (Raw)
                {
                    case string value:
                        it.Text = value;
                        break;
                    case byte[] value:
                        it.U8a = value;
                        break;
                    case Stream value:
                        it.Stream = value;
                        break;
                }
            })), true).Value;
        }
    }
    public override MetaBody MetaBody { get; set; }

    public IpcBodySender(object raw, Ipc ipc)
    {

        Raw = raw;
        SenderIpc = ipc;

        MetaBody = BodyAsMeta(Raw, SenderIpc);

        /// init
        CACHE.Raw_ipcBody_WMap.TryAdd(raw, this);
        IPC.UsableByIpc(ipc, this);
    }


    private static IpcBody s_fromAny(object raw, Ipc ipc) =>
        CACHE.Raw_ipcBody_WMap.TryGetValue(raw, out var ipcBody) ? ipcBody : new IpcBodySender(raw, ipc);
    public static IpcBody FromText(string raw, Ipc ipc) => FromBinary(raw.ToUtf8ByteArray(), ipc);
    public static IpcBody FromBase64(string raw, Ipc ipc) => s_fromAny(raw, ipc);
    public static IpcBody FromBinary(byte[] raw, Ipc ipc) => s_fromAny(raw, ipc);
    public static IpcBody FromStream(Stream raw, Ipc ipc) => s_fromAny(raw, ipc);

    private static ConditionalWeakTable<Stream, string> s_streamIdWM = new();

    private static int s_stream_id_acc = 0;

    private static string s_getStreamId(Stream stream) => s_streamIdWM.GetValueOrPut(stream, () =>
    {
        return string.Format("rs-{0}", Interlocked.Increment(ref s_stream_id_acc));
    });


    private MetaBody BodyAsMeta(object body, Ipc ipc) => body switch
    {
        string value => MetaBody.FromText(ipc.Uid, value),
        byte[] value => MetaBody.FromBinary(ipc, value),

        Stream value => StreamAsMeta(value, ipc),
        _ => throw new Exception(string.Format("invalid body type {0}", body)),
    };

    /// <summary>
    /// _streamStatusSignal 作为 BlockBuffer，它只能同时有一个在读取
    /// 所以这里定义一个Signal，分发成事件
    /// </summary>
    Signal<StreamStatusSignal>? streamStatusSignal;

    private MetaBody StreamAsMeta(Stream stream, Ipc ipc)
    {
        var stream_id = s_getStreamId(stream);

        Console.Log("StreamAsMeta", "sender/INIT/{0:H} {1}", stream, stream_id);

        Task.Run(async () =>
        {
            await foreach (var signal in _streamStatusSignal.ReceiveAllAsync())
            {
                streamStatusSignal?.Emit(signal);
            }
        }).Background();

        Task.Run(async () =>
        {
            /**
             * 只有等到 Pulling 指令的时候才能读取并发送
             */
            var pullingPo = new PromiseOut<Unit>();

            Task.Run(async () =>
            {
                streamStatusSignal += async (signal, self) =>
                {
                    switch (signal)
                    {
                        case StreamStatusSignal.PULLING:
                            pullingPo.Resolve(unit);
                            break;
                        case StreamStatusSignal.PAUSED:
                            if (pullingPo.IsFinished)
                            {
                                pullingPo = new PromiseOut<Unit>();
                            }
                            break;
                        case StreamStatusSignal.ABORTED:
                            stream.Dispose();
                            _emitStreamClose();
                            break;
                    }
                };

            }).Background();


            // 等待流开始被拉取
            await pullingPo.WaitPromiseAsync();

            Console.Log("StreamAsMeta", "sender/PULLING/{0:H} {1}", stream, stream_id);
            try
            {
                /// 持续发送数据
                await foreach (var bytes in stream.ReadBytesStream())
                {
                    Console.Log("StreamAsMeta", "sender/READ/{0:H} {1} >> {2}", stream, stream_id, bytes.Length);

                    var ipcStreamData = IpcStreamData.FromBinary(stream_id, bytes);
                    foreach (Ipc ipc in _usedIpcMap.Keys)
                    {
                        await ipc.PostMessageAsync(ipcStreamData);
                    }
                    Console.Log("StreamAsMeta", "sender/WAITTING/{0:H} {1}", stream, stream_id);
                }
            }
            finally
            {
                /// 不论是不是被 aborted，都发送结束信号
                var ipcStreamEnd = new IpcStreamEnd(stream_id);

                foreach (Ipc ipc in _usedIpcMap.Keys)
                {
                    await ipc.PostMessageAsync(ipcStreamEnd);
                }
            }

        });

        // 写入第一帧数据
        var streamType = MetaBody.IPC_META_BODY_TYPE.STREAM_ID;
        MetaBody metaBody = default;

        if (stream is IPreReadableInputStream prestream && prestream.PreReadableSize > 0)
        {
            streamType = MetaBody.IPC_META_BODY_TYPE.STREAM_WITH_BINARY;
            var streamFirstData = new byte[prestream.PreReadableSize];
            stream.Read(streamFirstData, 0, prestream.PreReadableSize);
            stream.Flush();

            metaBody = new MetaBody(streamType, ipc.Uid, streamFirstData, stream_id);
        }

        if (streamType == MetaBody.IPC_META_BODY_TYPE.STREAM_ID)
        {
            metaBody = new MetaBody(streamType, ipc.Uid, "", stream_id);
        }

        return metaBody!.Also(it =>
        {
            // 流对象，写入缓存
            CACHE.MetaId_ipcBodySender_Map.TryAdd(it.MetaId, this);
            streamStatusSignal += async (signal, self) =>
            {
                if (signal == StreamStatusSignal.ABORTED)
                {
                    CACHE.MetaId_ipcBodySender_Map.Remove(it.MetaId);
                }
            };
        });
    }
}

