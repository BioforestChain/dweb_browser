using System.Collections.Concurrent;
using System.Threading.Tasks.Dataflow;

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

    public abstract IMicroModule Remote { get; set; }

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

    private readonly HashSet<Signal<IpcMessage, Ipc>> MessageSignal = new();
    public event Signal<IpcMessage, Ipc> OnMessage
    {
        add { if(value != null) lock (MessageSignal) { MessageSignal.Add(value); } }
        remove { lock (MessageSignal) { MessageSignal.Remove(value); } }
    }
    protected Task _OnMessageEmit(IpcMessage msg, Ipc ipc) => MessageSignal.Emit(msg, ipc).ForAwait();

    public abstract Task _doPostMessageAsync(IpcMessage data);

    private readonly HashSet<Signal<IpcRequest, Ipc>> RequestSignal = new();
    public event Signal<IpcRequest, Ipc> OnRequest
    {
        add { if(value != null) lock (RequestSignal) { RequestSignal.Add(value); } }
        remove { lock (RequestSignal) { RequestSignal.Remove(value); } }
    }

    private readonly HashSet<Signal<IpcResponse, Ipc>> ResponseSignal = new();
    public event Signal<IpcResponse, Ipc> OnResponse
    {
        add { if(value != null) lock (ResponseSignal) { ResponseSignal.Add(value); } }
        remove { lock (ResponseSignal) { ResponseSignal.Remove(value); } }
    }

    private readonly HashSet<Signal<IIpcStream, Ipc>> StreamSignal = new();
    public event Signal<IIpcStream, Ipc> OnStream
    {
        add { if(value != null) lock (StreamSignal) { StreamSignal.Add(value); } }
        remove { lock (StreamSignal) { StreamSignal.Remove(value); } }
    }

    private readonly HashSet<Signal<IpcEvent, Ipc>> EventSignal = new();
    public event Signal<IpcEvent, Ipc> OnEvent
    {
        add { if(value != null) lock (EventSignal) { EventSignal.Add(value); } }
        remove { lock (EventSignal) { EventSignal.Remove(value); } }
    }

    private void ClearSignal()
    {
        // ipc关闭时，清空event signal
        MessageSignal.Clear();
        RequestSignal.Clear();
        ResponseSignal.Clear();
        StreamSignal.Clear();
        EventSignal.Clear();
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
        await CloseSignal.EmitAndClear();

        await Destroy(false);
    }

    public bool IsClosed
    {
        get { return _closed; }
    }

    private readonly HashSet<Signal> CloseSignal = new();
    public event Signal OnClose
    {
        add { if(value != null) lock (CloseSignal) { CloseSignal.Add(value); } }
        remove { lock (CloseSignal) { CloseSignal.Remove(value); } }
    }

    private readonly HashSet<Signal> DestorySignal = new();
    public event Signal OnDestory
    {
        add { if(value != null) lock (DestorySignal) { DestorySignal.Add(value); } }
        remove { lock (DestorySignal) { DestorySignal.Remove(value); } }
    }

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

        await DestorySignal.EmitAndClear();
    }

    public Ipc()
    {
        /// 这里建立起一个独立的顺序队列，目的是避免处理阻塞
        /// TODO 这里不应该使用 UNLIMITED，而是压力到一定程度方向发送限流的指令
        var streamChannel = new BufferBlock<(IIpcStream, Ipc)>(new DataflowBlockOptions { BoundedCapacity = DataflowBlockOptions.Unbounded });

        OnMessage += async (ipcMessage, ipc, _self) =>
        {
            switch (ipcMessage)
            {
                case IpcRequest ipcRequest:
                    _ = RequestSignal.Emit(ipcRequest, ipc); // 不 Await
                    break;
                case IpcResponse ipcResponse:
                    _ = ResponseSignal.Emit(ipcResponse, ipc); // 不 Await
                    break;
                case IpcEvent ipcEvent:
                    _ = EventSignal.Emit(ipcEvent, ipc); // 不 Await
                    break;
                case IIpcStream ipcStream:
                    streamChannel.Post((ipcStream, ipc));
                    break;
            }
        };

        //_ = Task.Factory.StartNew(async () =>
        //{
        //    await foreach (var (ipcStreamMessage, ipc) in streamChannel.ReceiveAllAsync())
        //    {
        //        await (StreamSignal.Emit(ipcStreamMessage, ipc)).ForAwait();
        //    }
        //}, TaskCreationOptions.LongRunning).NoThrow();

        _ = Task.Run(async () =>
        {
            await foreach (var (ipcStreamMessage, ipc) in streamChannel.ReceiveAllAsync())
            {
                await StreamSignal.Emit(ipcStreamMessage, ipc).ForAwait();
            }
        }).NoThrow();

        OnClose += (_) =>
        {
            ClearSignal();
            streamChannel.Complete();
            return null;
        };

    }

    private readonly LazyBox<ConcurrentDictionary<int, PromiseOut<IpcResponse>>> _reqResMap = new();

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
            var reqResMap = new ConcurrentDictionary<int, PromiseOut<IpcResponse>>();
            OnResponse += async (ipcResponse, ipc, self) =>
            {
                if (!reqResMap.Remove(ipcResponse.ReqId, out var res))
                {
                    throw new Exception(string.Format("no found response by req_id: {0}", ipcResponse.ReqId));
                }

                res.Resolve(ipcResponse);
            };
            return reqResMap;
        }).TryAdd(ipcRequest.ReqId, result);
        await PostMessageAsync(ipcRequest);
        return await result.WaitPromiseAsync();
    }

    public Task<PureResponse> Request(string url) =>
        Request(new PureRequest(url, IpcMethod.Get));
    public Task<PureResponse> Request(Uri url) =>
        Request(new PureRequest(url.ToString(), IpcMethod.Get));
    public async Task<PureResponse> Request(PureRequest request) =>
        (await Request(request.ToIpcRequest(AllocReqId(), this))).ToPureResponse();

    public static int AllocReqId()
    {
        var reqId = Interlocked.Increment(ref s_req_id_acc);
        Console.Log("AllocReqId", "{0}", reqId);
        return reqId;
    }
}

