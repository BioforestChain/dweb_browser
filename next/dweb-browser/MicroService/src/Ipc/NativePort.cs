using System.Threading.Tasks.Dataflow;

namespace DwebBrowser.MicroService;

public class SharedCloseSignal
{
    private readonly HashSet<Signal> CloseSignal = new();
    public event Signal OnClose
    {
        add { if (value != null) lock (CloseSignal) { CloseSignal.Add(value); } }
        remove { lock (CloseSignal) { CloseSignal.Remove(value); } }
    }
    private bool Closed = false;
    public bool isClosed()
    {
        lock (CloseSignal)
        {
            return Closed;
        }
    }
    public Task EmitClose()
    {
        lock (CloseSignal)
        {
            if (Closed)
            {
                return Task.CompletedTask;
            }
            Closed = true;
            return CloseSignal.EmitAndClear();
        }
    }
}

public class NativePort<I, O>
{
    static readonly Debugger Console = new("NativePort");
    private BufferBlock<I> _channel_in { get; set; }
    private BufferBlock<O> _channel_out { get; set; }
    private SharedCloseSignal _cs { init; get; }

    public NativePort(BufferBlock<I> channel_in, BufferBlock<O> channel_out, SharedCloseSignal cs)
    {
        _channel_in = channel_in;
        _channel_out = channel_out;
        _cs = cs;
    }

    private static int s_uid_acc = 0;

    private readonly int _uid = Interlocked.Increment(ref s_uid_acc);

    public override string ToString() => string.Format("#p{0}", _uid);

    private bool _started { get; set; } = false;

    public async Task StartAsync()
    {
        if (_started)
        {
            return;
        }
        else
        {
            _started = true;
        }

        if (_cs.isClosed())
        {
            await _close();
        }
        else
        {
            _cs.OnClose += (_) => _close();
        }

        Console.Log("Start", "port-message-start/{0}", this);
        await foreach (I message in _channel_in.ReceiveAllAsync())
        {
            Console.Log("Start", "port-message-in/{0} {1}", this, message);
            await (MessageSignal.Emit(message)).ForAwait();
            Console.Log("Start", "port-message-waiting/{0}", this);
        }

        //OnMessage = null;
        Console.Log("Start", "port-message-end/{0}", this);
    }

    private readonly HashSet<Signal> CloseSignal = new();
    public event Signal OnClose
    {
        add { if (value != null) lock (CloseSignal) { CloseSignal.Add(value); } }
        remove { lock (CloseSignal) { CloseSignal.Remove(value); } }
    }

    public Task Close()
    {
        return _cs.EmitClose();
    }

    private async Task _close()
    {
        /// 关闭输出就行了
        _channel_out.Complete();
        await CloseSignal.EmitAndClear();
        Console.Log("Close", "port-closed/{0}", this);
    }

    private readonly HashSet<Signal<I>> MessageSignal = new();
    public event Signal<I> OnMessage
    {
        add { if (value != null) lock (MessageSignal) { MessageSignal.Add(value); } }
        remove { lock (MessageSignal) { MessageSignal.Remove(value); } }
    }

    /**
     * <summary>
     * 发送消息，这个默认会阻塞
     * </summary>
     */
    public async Task PostMessageAsync(O msg)
    {
        Console.Log("PostMessage", "message-out/{0} >> {1}", this, msg);
        var success = await _channel_out.SendAsync(msg);

        if (!success)
        {
            Console.Log("PostMessage", "handle the closed channel case!");
        }
    }
}

public class NativeMessageChannel<T1, T2>
{
    /**
     * 默认锁住，当它解锁的时候，意味着通道关闭
     */
    private readonly SharedCloseSignal _cs = new();
    private BufferBlock<T1> _channel1 = new(new DataflowBlockOptions { BoundedCapacity = DataflowBlockOptions.Unbounded });
    private BufferBlock<T2> _channel2 = new(new DataflowBlockOptions { BoundedCapacity = DataflowBlockOptions.Unbounded });
    public NativePort<T1, T2> Port1;
    public NativePort<T2, T1> Port2;

    public NativeMessageChannel()
    {
        Port1 = new NativePort<T1, T2>(_channel1, _channel2, _cs);
        Port2 = new NativePort<T2, T1>(_channel2, _channel1, _cs);
    }
}