using System.Threading.Tasks.Dataflow;
using DwebBrowser.Helper;
using DwebBrowser.MicroService.Http;

namespace DwebBrowser.MicroService;
public abstract class Ipc
{
    static readonly Debugger Console = new("Ipc");
    private static int s_uid_acc = 0;
    private static int s_req_id_acc = 0;

    public int Uid { get; set; } = Interlocked.Increment(ref s_uid_acc);

    /**
     * <summary>
     * 是否支持 Cbor 协议传输：
     * 需要同时满足两个条件：通道支持直接传输二进制；通达支持 Cbor 的编解码
     * </summary>
     */
    public bool SupportCbor { get; set; } = false;

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

    public MicroModule? AsRemoteInstance() => Remote is MicroModule microModule ? microModule : null;

    public abstract string Role { get; }

    public override string ToString() => string.Format("#i{0}", Uid);

    public async Task PostMessageAsync(IpcMessage message)
    {
        if (_closed)
        {
            return;
        }

        await _doPostMessageAsync(message);
    }

    public event Signal<IpcMessage, Ipc>? OnMessage;
    protected Task _OnMessageEmit(IpcMessage msg, Ipc ipc) => (OnMessage?.Emit(msg, ipc)).ForAwait();

    public abstract Task _doPostMessageAsync(IpcMessage data);

    public event Signal<IpcRequest, Ipc>? OnRequest;

    public event Signal<IpcResponse, Ipc>? OnResponse;

    public event Signal<IpcStream, Ipc>? OnStream;

    public event Signal<IpcEvent, Ipc>? OnEvent;

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

    public event Signal? OnClose;

    public event Signal? OnDestory;

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

        OnDestory = null;
    }

    public Ipc()
    {
        /// 这里建立起一个独立的顺序队列，目的是避免处理阻塞
        /// TODO 这里不应该使用 UNLIMITED，而是压力到一定程度方向发送限流的指令
        var streamChannel = new BufferBlock<(IpcStream, Ipc)>(new DataflowBlockOptions { BoundedCapacity = DataflowBlockOptions.Unbounded });

        OnMessage += async (ipcMessage, ipc, _) =>
        {
            switch (ipcMessage)
            {
                case IpcRequest ipcRequest:
                    OnRequest?.Emit(ipcRequest, ipc); // 不 Await
                    break;
                case IpcResponse ipcResponse:
                    OnResponse?.Emit(ipcResponse, ipc); // 不 Await
                    break;
                case IpcEvent ipcEvent:
                    OnEvent?.Emit(ipcEvent, ipc); // 不 Await
                    break;
                case IpcStream ipcStream:
                    streamChannel.Post((ipcStream, ipc));
                    break;
            }
        };


        Task.Run(async () =>
        {
            await foreach (var (ipcStreamMessage, ipc) in streamChannel.ReceiveAllAsync())
            {
                await (OnStream?.Emit(ipcStreamMessage, ipc)).ForAwait();
            }
        }).Background();

        OnClose += (_) =>
        {
            streamChannel.Complete();
            return null;
        };

    }

    private LazyBox<Dictionary<int, PromiseOut<IpcResponse>>> _reqResMap = new();

    /// <summary>
    /// 发送请求
    /// </summary>
    /// <param name="ipcRequest"></param>
    /// <returns></returns>
    /// <exception cref="Exception"></exception>
    public async Task<IpcResponse> Request(IpcRequest ipcRequest)
    {
        var result = new PromiseOut<IpcResponse>();
        _reqResMap.GetOrPut(() =>
        {
            var reqResMap = new Dictionary<int, PromiseOut<IpcResponse>>();
            OnResponse += async (ipcResponse, ipc, self) =>
            {
                if (!reqResMap.Remove(ipcResponse.ReqId, out var res))
                {
                    throw new Exception(string.Format("no found response by req_id: {0}", ipcResponse.ReqId));
                }

                res.Resolve(ipcResponse);
            };
            return reqResMap;
        }).Add(ipcRequest.ReqId, result);
        await PostMessageAsync(ipcRequest);
        return await result.WaitPromiseAsync();
    }

    public Task<PureResponse> Request(string url) =>
        Request(new PureRequest(url, IpcMethod.Get));
    public Task<PureResponse> Request(Uri url) =>
        Request(new PureRequest(url.ToString(), IpcMethod.Get));
    public async Task<PureResponse> Request(PureRequest request) =>
        (await Request(request.ToIpcRequest(AllocReqId(), this))).ToPureResponse();

    public int AllocReqId()
    {
        var reqId = Interlocked.Increment(ref s_req_id_acc);
        Console.Log("AllocReqId", "{0}", reqId);
        return reqId;
    }
}

