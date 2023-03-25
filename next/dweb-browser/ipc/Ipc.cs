using System.Net.Http;

namespace ipc;

using Mmid = String;


public abstract class Ipc
{
    private static int _uid_acc = 1;
    private static int _req_id_acc = 0;

    public int Uid { get; set; } = Interlocked.Exchange(ref _uid_acc, Interlocked.Increment(ref _uid_acc));

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
        public Mmid mmid { get; set; }
    }

    // TODO: MicroModule还未实现
    //public MicroModule AsRemoteInstance()

    public abstract string Role { get; set; }

    public override string ToString() => $"#i{Uid}";

    public async Task PostMessageAsync(IpcMessage message)
    {
        if (_closed)
        {
            return;
        }

        await _doPostMessageAsync(message);
    }

    public async Task PostResponseAsync(int req_id, HttpResponseMessage response)
    {
        await PostMessageAsync(IpcResponse.FromResponse(req_id, response, this));
    }

    protected Signal<IpcMessageArgs> _messageSigal = new Signal<IpcMessageArgs>();

    public Func<bool> OnMessage(Func<IpcMessageArgs, object?> cb) => _messageSigal.Listen(cb);

    public abstract Task _doPostMessageAsync(IpcMessage data);

    private Signal<IpcRequestMessageArgs> _requestSignal
    {
        get
        {
            return new Lazy<Signal<IpcRequestMessageArgs>>(new Func<Signal<IpcRequestMessageArgs>>(() =>
                new Signal<IpcRequestMessageArgs>().Also(signal =>
                {
                    _messageSigal.Listen(new Func<IpcMessageArgs, object?>(args =>
                    {
                        if (args.Message is IpcRequest ipcRequest)
                        {
                            signal.EmitAsync(new IpcRequestMessageArgs(ipcRequest, args.Mipc));

                            return true;
                        }

                        return false;
                    }));
                }))).Value;
        }
    }

    public Func<bool> OnRequest(Func<IpcRequestMessageArgs, object?> cb) => _requestSignal.Listen(cb);

    private Signal<IpcResponseMessageArgs> _responseSignal
    {
        get
        {
            return new Lazy<Signal<IpcResponseMessageArgs>>(new Func<Signal<IpcResponseMessageArgs>>(() =>
                new Signal<IpcResponseMessageArgs>().Also(signal =>
                {
                    _messageSigal.Listen(new Func<IpcMessageArgs, object?>(args =>
                    {
                        if (args.Message is IpcResponse ipcResponse)
                        {
                            signal.EmitAsync(new IpcResponseMessageArgs(ipcResponse, args.Mipc));

                            return true;
                        }

                        return false;
                    }));
                }))).Value;
        }
    }

    public Func<bool> OnResponse(Func<IpcResponseMessageArgs, object?> cb) => _responseSignal.Listen(cb);

    private Signal<IpcStreamMessageArgs> _streamSignal
    {
        get
        {
            return new Lazy<Signal<IpcStreamMessageArgs>>(new Func<Signal<IpcStreamMessageArgs>>(() =>
                new Signal<IpcStreamMessageArgs>().Also(signal =>
                {
                    _messageSigal.Listen(new Func<IpcMessageArgs, object?>(args =>
                    {
                        if (args.Message is IpcStream ipcStream)
                        {
                            signal.EmitAsync(new IpcStreamMessageArgs(ipcStream, args.Mipc));

                            return true;
                        }

                        return false;
                    }));
                }))).Value;
        }
    }

    public Func<bool> OnStream(Func<IpcStreamMessageArgs, object?> cb) => _streamSignal.Listen(cb);

    private Signal<IpcEventMessageArgs> _eventSignal
    {
        get
        {
            return new Lazy<Signal<IpcEventMessageArgs>>(new Func<Signal<IpcEventMessageArgs>>(() =>
                new Signal<IpcEventMessageArgs>().Also(signal =>
                {
                    _messageSigal.Listen(new Func<IpcMessageArgs, object?>(args =>
                    {
                        if (args.Message is IpcEvent ipcEvent)
                        {
                            signal.EmitAsync(new IpcEventMessageArgs(ipcEvent, args.Mipc));

                            return true;
                        }

                        return false;
                    }));
                }))).Value;
        }
    }

    public Func<bool> OnEvent(Func<IpcEventMessageArgs, object?> cb) => _eventSignal.Listen(cb);


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

    private SimpleSignal _closeSignal = new SimpleSignal();

    public Func<bool> OnClose(Func<byte, object?> cb) => _closeSignal.Listen(cb);

    private SimpleSignal _destroySignal = new SimpleSignal();

    public Func<bool> OnDestory(Func<byte, object?> cb) => _destroySignal.Listen(cb);

    private bool _destroyed = false;
    public bool IsDestroy
    {
        get { return _destroyed; }
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

        await _destroySignal.EmitAsync();
        _destroySignal.Clear();
    }

    /**
     * 发送请求
     */
    public async Task<HttpResponseMessage> Request(string url) =>
        await this.Request(new HttpRequestMessage(HttpMethod.Get, new Uri(url)));

    public async Task<HttpResponseMessage> Request(Uri url) =>
        await this.Request(new HttpRequestMessage(HttpMethod.Get, url));

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
                            var ipcResponse = arg.response;
                            var res = reqResMap[ipcResponse.ReqId];

                            if (res is null)
                            {
                                throw new Exception($"no found response by req_id: {ipcResponse.ReqId}");
                            }

                            res.Resolve(ipcResponse);
                            return null;
                        });
                    });
                })).Value;
        }
    }

    public async Task<IpcResponse> Request(IpcRequest ipcRequest)
    {
        var result = new PromiseOut<IpcResponse>();
        _reqResMap[ipcRequest.ReqId] = result;
        await PostMessageAsync(ipcRequest);
        return result.WaitPromise();
    }

    public async Task<HttpResponseMessage> Request(HttpRequestMessage request) =>
        (await this.Request(IpcRequest.FromRequest(AllocReqId(), request, this))).ToResponse();

    public int AllocReqId() => Interlocked.Exchange(ref _req_id_acc, Interlocked.Increment(ref _req_id_acc));
}

