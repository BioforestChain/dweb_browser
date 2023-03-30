using System.Threading.Tasks.Dataflow;

namespace ipc;

using IpcMessageArgs = Tuple<IpcMessage, Ipc>;
using IpcRequestMessageArgs = Tuple<IpcRequest, Ipc>;
using IpcResponseMessageArgs = Tuple<IpcResponse, Ipc>;
using IpcEventMessageArgs = Tuple<IpcEvent, Ipc>;
using IpcStreamMessageArgs = Tuple<IpcStream, Ipc>;

public abstract class Ipc
{
    private static int s_uid_acc = 1;
    private static int s_req_id_acc = 0;

    public int Uid { get; set; } = Interlocked.Exchange(ref s_uid_acc, Interlocked.Increment(ref s_uid_acc));

    /**
     * <summary>
     * 是否支持 messagePack 协议传输：
     * 需要同时满足两个条件：通道支持直接传输二进制；通达支持 MessagePack 的编解码
     * </summary>
     */
    public bool SupportMessagePack { get; set; } = false;

    /**
     * <summary>
     * 是否支持 Protobuf 协议传输：
     * 需要同时满足两个条件：通道支持直接传输二进制；通达支持 Protobuf 的编解码
     * </summary>
     */
    public bool SupportProtobuf { get; set; } = false;

    /**
     * <summary>
     * 是否支持结构化内存协议传输：
     * 就是说不需要对数据手动序列化反序列化，可以直接传输内存对象
     * </summary>
     */
    public bool SupportRaw { get; set; } = false;

    /** <summary>是否支持 二进制 传输</summary> */
    public bool SupportBinary { get; set; } = false;

    public abstract MicroModuleInfo Remote { get; set; }

    public interface MicroModuleInfo
    {
        public Mmid Mmid { get; init; }
    }

    // TODO: MicroModule还未实现
    //public MicroModule AsRemoteInstance()

    public abstract string Role { get; }

    public override string ToString() => $"#i{Uid}";

    public async Task PostMessageAsync(IpcMessage message)
    {
        if (_closed)
        {
            return;
        }

        await _doPostMessageAsync(message);
    }

    public Task PostResponseAsync(int req_id, HttpResponseMessage response) =>
        PostMessageAsync(IpcResponse.FromResponse(req_id, response, this));

    protected delegate Task _messageSignalHandler(IpcMessageArgs ipcMessageArgs);
    protected event _messageSignalHandler _messageSignal = null!;

    public abstract Task _doPostMessageAsync(IpcMessage data);

    public delegate Task RequestSignalHandler(IpcRequestMessageArgs ipcRequestMessageArgs);
    public event RequestSignalHandler RequestSignal = null!;

    public void OnRequest(RequestSignalHandler cb)
    {
        RequestSignal += cb;
        _messageSignal += async args =>
        {
            if (args.Item1 is IpcRequest ipcRequest)
            {
                await RequestSignal?.Invoke(Tuple.Create(ipcRequest, args.Item2))!;
            }
        };
    }

    public delegate Task ResponseSignalHandler(IpcResponseMessageArgs ipcResponseMessageArgs);
    public event ResponseSignalHandler ResponseSignal = null!;

    public void OnResponse(ResponseSignalHandler cb)
    {
        ResponseSignal += cb;
        _messageSignal += async args =>
        {
            if (args.Item1 is IpcResponse ipcResponse)
            {
                await ResponseSignal?.Invoke(Tuple.Create(ipcResponse, args.Item2))!;
            }
        };
    }

    public delegate Task StreamSignalHanlder(IpcStreamMessageArgs ipcStreamMessageArgs);

    public event StreamSignalHanlder StreamSignal = null!;

    public void OnStream(StreamSignalHanlder cb)
    {
        StreamSignal += cb;

        /// 这里建立起一个独立的顺序队列，目的是避免处理阻塞
        /// TODO 这里不应该使用 UNLIMITED，而是压力到一定程度方向发送限流的指令
        var streamChannel = new BufferBlock<IpcStreamMessageArgs>();
        Task.Run(async () =>
        {
            await foreach (IpcStreamMessageArgs message in streamChannel.ReceiveAllAsync())
            {
                //await signal.EmitAsync(message);
                await StreamSignal?.Invoke(message)!;
            }
        });

        _messageSignal += async args =>
        {
            if (args.Item1 is IpcStream ipcStream)
            {
                await streamChannel.SendAsync(new IpcStreamMessageArgs(ipcStream, args.Item2));
            }
        };
    }

    public delegate Task EventSignalHandler(IpcEventMessageArgs ipcEventMessageArgs);
    public EventSignalHandler EventSignal = null!;

    public void OnEvent(EventSignalHandler cb)
    {
        EventSignal += cb;

        _messageSignal += async args =>
        {
            if (args.Item1 is IpcEvent ipcEvent)
            {
                await EventSignal?.Invoke(Tuple.Create(ipcEvent, args.Item2))!;
            }
        };
    }


    public abstract Task DoClose();

    private bool _closed { get; set; } = false;

    public async Task Close()
    {
        if (_closed)
        {
            return;
        }

        _closed = true;
        await DoClose();
    }

    public bool IsClosed
    {
        get { return _closed; }
    }

    public delegate void CloseSignalHandler();
    public CloseSignalHandler CloseSignal = null!;

    public void OnClose(CloseSignalHandler cb) => CloseSignal += cb;

    public delegate void DestroySignalHandler();
    public DestroySignalHandler DestroySignal = null!;

    public void OnDestory(DestroySignalHandler cb) => DestroySignal += cb;

    private bool _destroyed = false;
    public bool IsDestroy
    {
        get => _destroyed;
    }

    /**
     * 销毁实例
     */
    public async Task Destroy(bool close = true)
    {
        if (_destroyed)
        {
            return;
        }

        _destroyed = true;

        if (close)
        {
            await Close();
        }

        if (DestroySignal is not null)
        {
            DestroySignal.Invoke();
            foreach (DestroySignalHandler cb in DestroySignal.GetInvocationList().Cast<DestroySignalHandler>())
            {
                DestroySignal -= cb;
            }
        }
    }

    /**
     * 发送请求
     */
    public Task<HttpResponseMessage> Request(string url) =>
        this.Request(new HttpRequestMessage(HttpMethod.Get, new Uri(url)));

    public Task<HttpResponseMessage> Request(Uri url) =>
        this.Request(new HttpRequestMessage(HttpMethod.Get, url));

    private Dictionary<int, PromiseOut<IpcResponse>> _reqResMap
    {
        get
        {
            return new Lazy<Dictionary<int, PromiseOut<IpcResponse>>>(
                new Func<Dictionary<int, PromiseOut<IpcResponse>>>(() =>
                {
                    return new Dictionary<int, PromiseOut<IpcResponse>>().Also(reqResMap =>
                    {
                        OnResponse((IpcResponseMessageArgs arg) =>
                        {
                            var ipcResponse = arg.Item1;
                            var res = reqResMap[ipcResponse.ReqId];

                            if (res is null)
                            {
                                throw new Exception($"no found response by req_id: {ipcResponse.ReqId}");
                            }

                            res.Resolve(ipcResponse);
                            return Task.CompletedTask;
                        });
                    });
                }), true).Value;
        }
    }

    public async Task<IpcResponse> Request(IpcRequest ipcRequest)
    {
        var result = new PromiseOut<IpcResponse>();
        _reqResMap[ipcRequest.ReqId] = result;
        await PostMessageAsync(ipcRequest);
        return await result.WaitPromiseAsync();
    }

    public async Task<HttpResponseMessage> Request(HttpRequestMessage request) =>
        (await this.Request(IpcRequest.FromRequest(AllocReqId(), request, this))).ToResponse();

    public int AllocReqId() => Interlocked.Exchange(ref s_req_id_acc, Interlocked.Increment(ref s_req_id_acc));
}

