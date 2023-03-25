using System.Collections.Concurrent;
using System.Threading.Tasks.Dataflow;

namespace ipc;

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
    public override object? Raw { get; }
    public Ipc SenderIpc { get; set; }

    public bool IsStream
    {
        get { return new Lazy<bool>(new Func<bool>(() => Raw is Stream)).Value; }
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

    private void _emitStreamClose()
    {
        _isStreamOpened = true;
        _isStreamClosed = true;
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

    private BufferBlock<StreamStatusSignal> _streamStatusSignal = new BufferBlock<StreamStatusSignal>();

    class IPC
    {
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
                var ipcBodySender = this.Get(streamId);

                if (ipcBodySender is not null)
                {
                    _map.Remove(streamId);
                }

                if (_map.Count == 0)
                {
                    await this._destroySignal.EmitAsync(0);
                    this._destroySignal.Clear();
                }

                return ipcBodySender;
            }

            private SimpleSignal _destroySignal = new SimpleSignal();

            public Func<bool> OnDetroy(Func<byte, object?> cb) => _destroySignal.Listen(cb);
        }

        private static Dictionary<Ipc, UsableIpcBodyMapper> _ipcUsableIpcBodyMap = new Dictionary<Ipc, UsableIpcBodyMapper>();

        //class IpcExtensions
        //{
        //    public UsableIpcBodyMapper _getUsableIpcBodyMap(this Ipc ipc)
        //    {
        //        //var usableIpcBodyMapper = _ipcUsableIpcBodyMap[ipc];

        //        //if (usableIpcBodyMapper is null)
        //        //{
        //        //    usableIpcBodyMapper = new UsableIpcBodyMapper().Also(mapper =>
        //        //    {
        //        //        var off = ipc.OnStream((IpcStreamMessageArgs arg) => arg.stream switch
        //        //            {
        //        //                IpcStreamPulling message => mapper.Get(message.StreamId)?.use
        //        //            });
        //        //    });
        //        //    _ipcUsableIpcBodyMap.Add(ipc);
        //        //}
        //    }
        //}

        public void UsableByIpc(Ipc ipc, IpcBodySender ipcBody)
        {
            if (!ipcBody.IsStream || ipcBody._isStreamOpened)
            {
                return;
            }

            var streamId = ipcBody.MetaBody.StreamId!;
            //var usableIpcBodyMapper = ipc._getUsableIpcBodyMap();
        }
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
                        it.BodyStream = value;
                        break;
                }
            }))).Value;
        }
    }
    public override SMetaBody MetaBody { get; set; }

    public IpcBodySender(object raw, Ipc ipc)
    {
        CACHE.Raw_ipcBody_WMap.Add(raw, this);


        Raw = raw;
        SenderIpc = ipc;

        MetaBody = BodyAsMeta(Raw, SenderIpc);
    }


    public static IpcBodySender From(object raw, Ipc ipc) => new IpcBodySender(raw, ipc);
    private static Lazy<Dictionary<Stream, string>> _streamIdWM =
        new Lazy<Dictionary<Stream, string>>(() => new Dictionary<Stream, string>());

    private static int _stream_id_acc = 1;

    private static string _getStreamId(Stream stream) => _streamIdWM.Value.Let(it =>
        {
            var streamId = it[stream];

            if (streamId is null)
            {
                streamId = $"rs-{Interlocked.Exchange(ref _stream_id_acc, Interlocked.Increment(ref _stream_id_acc))}";
            }

            return streamId;
        });



    private SMetaBody BodyAsMeta(object body, Ipc ipc) => body switch
    {
        string value => SMetaBody.FromText(ipc.Uid, value),
        byte[] value => SMetaBody.FromBinary(ipc, value),

        Stream value => StreamAsMeta(value, ipc),
        _ => throw new Exception($"invalid body type {body}"),

    };

    private SMetaBody StreamAsMeta(Stream stream, Ipc ipc)
    {
        var stream_id = _getStreamId(stream);

        Console.WriteLine($"sender/INIT/{stream}", stream_id);

        Task.Run(() =>
        {
            /**
             * 只有等到 Pulling 指令的时候才能读取并发送
             */
            var pullingPo = new PromiseOut<bool>();

            Task.Run(() =>
            {
                switch (_streamStatusSignal.Receive())
                {
                    case StreamStatusSignal.PULLING:
                        pullingPo.Resolve(true);
                        break;
                    case StreamStatusSignal.PAUSED:
                        if (pullingPo.IsFinished)
                        {
                            pullingPo = new PromiseOut<bool>();
                        }
                        break;
                    case StreamStatusSignal.ABORTED:
                        stream.Dispose();
                        _emitStreamClose();
                        break;
                }
            });

            /// 持续发送数据
            while (true)
            {
                // 等待流开始被拉取
                pullingPo.WaitPromise();

                Console.WriteLine($"sender/PULLING/{stream}", stream_id);
                //switch (stream.CanRead)
                //{

                //}

                switch (stream.ReadByte())
                {
                    case -1:
                        Console.WriteLine($"sender/END/{stream}", $"-1 >> {stream_id}");

                        /// 不论是不是被 aborted，都发送结束信号
                        var message = new IpcStreamEnd(stream_id);
                        break;
                    case 0:
                        break;
                }
            }
        });

        return new SMetaBody();
    }
}

