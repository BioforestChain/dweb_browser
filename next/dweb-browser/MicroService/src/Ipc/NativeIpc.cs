using System.Threading.Tasks.Dataflow;

namespace DwebBrowser.MicroService;

public class NativeIpc : Ipc
{
    public NativePort<IpcMessage, IpcMessage> Port;
    public override IMicroModuleInfo Remote { get; set; }
    private IPC_ROLE _role_type { get; set; }

    public NativeIpc(NativePort<IpcMessage, IpcMessage> port, IMicroModuleInfo remote, IPC_ROLE role)
    {
        Port = port;
        Remote = remote;
        _role_type = role;

        SupportRaw = true;
        SupportBinary = true;

        Port.OnMessage += async (message, _) =>
        {
            await _OnMessageEmit(message, this);
        };

        _ = Task.Run(Port.StartAsync).NoThrow();
    }

    public override string Role
    {
        get
        {
            return _role_type.ToString();
        }
    }

    public override string ToString() => base.ToString() + "@NativeIpc";

    public override Task _doPostMessageAsync(IpcMessage data) => Port.PostMessageAsync(data);

    public override Task DoClose() => Task.Run(() => Port.Close()).NoThrow();
}

public class NativePort<I, O>
{
    static readonly Debugger Console = new("NativePort");
    private BufferBlock<I> _channel_in { get; set; }
    private BufferBlock<O> _channel_out { get; set; }
    private PromiseOut<Unit> _closePo = new PromiseOut<Unit>();

    public NativePort(BufferBlock<I> channel_in, BufferBlock<O> channel_out, PromiseOut<Unit> closePo)
    {
        _channel_in = channel_in;
        _channel_out = channel_out;
        _closePo = closePo;

        /**
         * 等待 close 信号被发出，那么就关闭出口、触发事件
         */
        _ = Task.Run(async () =>
        {
            await _closePo.WaitPromiseAsync();
            _channel_out.Complete();
            await (OnClose?.Emit()).ForAwait();
            Console.Log("OnClose", "port-closed/{0}", this);
        }).NoThrow();
    }

    private static int s_uid_acc = 0;

    private int _uid = Interlocked.Increment(ref s_uid_acc);

    public override string ToString() => string.Format("#p{0}", _uid);

    private bool _started { get; set; } = false;

    public async Task StartAsync()
    {
        if (_started || _closePo.IsFinished)
        {
            return;
        }
        else
        {
            _started = true;
        }

        Console.Log("Start", "port-message-start/{0}", this);
        await foreach (I message in _channel_in.ReceiveAllAsync())
        {
            Console.Log("Start", "port-message-in/{0} {1}", this, message);
            await (OnMessage?.Emit(message)).ForAwait();
            Console.Log("Start", "port-message-waiting/{0}", this);
        }

        Console.Log("Start", "port-message-end/{0}", this);
    }

    public event Signal? OnClose;

    public void Close()
    {
        if (!_closePo.IsFinished)
        {
            _closePo.Resolve(unit);
            Console.Log("Close", "port-closing/{0}", this);
        }
    }

    public event Signal<I>? OnMessage;

    /**
     * <summary>
     * 发送消息，这个默认会阻塞
     * </summary>
     */
    public async Task PostMessageAsync(O msg)
    {
        Console.Log("PostMessage", "message-out/{0} >> {1}", this, msg);
        var success = await _channel_out.SendAsync(msg);
    }
}

public class NativeMessageChannel<T1, T2>
{
    /**
     * 默认锁住，当它解锁的时候，意味着通道关闭
     */
    private PromiseOut<Unit> _closePo = new PromiseOut<Unit>();
    private BufferBlock<T1> _channel1 = new BufferBlock<T1>(new DataflowBlockOptions { BoundedCapacity = DataflowBlockOptions.Unbounded });
    private BufferBlock<T2> _channel2 = new BufferBlock<T2>(new DataflowBlockOptions { BoundedCapacity = DataflowBlockOptions.Unbounded });
    public NativePort<T1, T2> Port1;
    public NativePort<T2, T1> Port2;

    public NativeMessageChannel()
    {
        Port1 = new NativePort<T1, T2>(_channel1, _channel2, _closePo);
        Port2 = new NativePort<T2, T1>(_channel2, _channel1, _closePo);
    }
}