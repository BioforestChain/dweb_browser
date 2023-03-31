using System.Threading.Tasks.Dataflow;

namespace ipc;

public class NativeIpc : Ipc
{
    public NativePort<IpcMessage, IpcMessage> Port;
    public override MicroModuleInfo Remote { get; set; }
    private IPC_ROLE _role_type { get; set; }

    public NativeIpc(NativePort<IpcMessage, IpcMessage> port, Ipc.MicroModuleInfo remote, IPC_ROLE role)
    {
        Port = port;
        Remote = remote;
        _role_type = role;

        SupportRaw = true;
        SupportBinary = true;

        Port.OnMessage(async (message) =>
        {
            OnMessageEmit(message, this);
        });

        Task.Run(Port.StartAsync);
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

    public override Task DoClose() => Task.Run(() => Port.Close());
}

public class NativePort<I, O>
{
    private BufferBlock<I> _channel_in { get; set; }
    private BufferBlock<O> _channel_out { get; set; }
    private PromiseOut<bool> _closePo = new PromiseOut<bool>();

    public NativePort(BufferBlock<I> channel_in, BufferBlock<O> channel_out, PromiseOut<bool> closePo)
    {
        _channel_in = channel_in;
        _channel_out = channel_out;
        _closePo = closePo;

        /**
         * 等待 close 信号被发出，那么就关闭出口、触发事件
         */
        Task.Run(async () =>
        {
            await _closePo.WaitPromiseAsync();
            _channel_out.Complete();
            OnCloseEvent.Emit();
            Console.WriteLine($"port-closed/{this}");
        });
    }

    private static int s_uid_acc = 1;

    private int _uid = Interlocked.Exchange(ref s_uid_acc, Interlocked.Increment(ref s_uid_acc));

    public override string ToString() => $"#p{_uid}";

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

        Console.WriteLine($"port-message-start/{this}");
        await foreach (I message in _channel_in.ReceiveAllAsync())
        {
            Console.WriteLine($"port-message-in/{this}");
            OnMessageHandler.Emit(message);
            Console.WriteLine($"port-message-waiting/{this}");
        }

        Console.WriteLine($"port-message-end/{this}");
    }

    public event Signal? OnCloseEvent;

    public void OnClose(OnSimpleMessageHandler cb) => OnCloseEvent.Listen(cb);

    public void Close()
    {
        if (!_closePo.IsFinished)
        {
            _closePo.Resolve(true);
            Console.WriteLine($"port-closing/{this}");
        }
    }

    public event Signal<I> OnMessageHandler;

    /**
     * <summary>
     * 发送消息，这个默认会阻塞
     * </summary>
     */
    public Task PostMessageAsync(O msg)
    {
        Console.WriteLine($"message-out/{this} >> {msg}");
        return _channel_out.SendAsync(msg);
    }

    /**
     * <summary>
     * 监听消息
     * </summary>
     */
    public void OnMessage(OnSingleMessageHandler<I> cb) => OnMessageHandler.Listen(cb);
}

public class NativeMessageChannel<T1, T2>
{
    /**
     * 默认锁住，当它解锁的时候，意味着通道关闭
     */
    private PromiseOut<bool> _closePo = new PromiseOut<bool>();
    private BufferBlock<T1> _channel1 = new BufferBlock<T1>();
    private BufferBlock<T2> _channel2 = new BufferBlock<T2>();
    public NativePort<T1, T2> Port1;
    public NativePort<T2, T1> Port2;

    public NativeMessageChannel()
    {
        Port1 = new NativePort<T1, T2>(_channel1, _channel2, _closePo);
        Port2 = new NativePort<T2, T1>(_channel2, _channel1, _closePo);
    }
}