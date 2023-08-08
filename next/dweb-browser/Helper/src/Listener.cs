using System.Threading.Tasks.Dataflow;

namespace DwebBrowser.Helper;

public class Listener
{
    private readonly HashSet<Signal> signal = new();
    public event Signal OnListener
    {
        add { if (value != null) lock (signal) { signal.Add(value); } }
        remove { lock (signal) { signal.Remove(value); } }
    }

    private readonly LazyBox<BufferBlock<Unit>> LazyChannel = new();
    private BufferBlock<Unit> SignalChannel => LazyChannel.GetOrPut(() =>
    {
        return new BufferBlock<Unit>(
            new DataflowBlockOptions
            {
                CancellationToken = Source.Token,
                BoundedCapacity = DataflowBlockOptions.Unbounded
            }).Also(it =>
            {
                Source.Token.Register(() =>
                {
                    it.Complete();
                });
            });
    });

    private readonly LazyBox<CancellationTokenSource> LazySource = new();
    private CancellationTokenSource Source => LazySource.GetOrPut(() => new CancellationTokenSource());

    public Listener ToFlow()
    {
        OnListener += async (self) =>
        {
            await SignalChannel.SendAsync(Unit.Default);

            Source.Token.Register(() =>
            {
                OnListener -= self;
            });
        };

        return this;
    }

    public int Size => signal.Count;

    public Task AddListenerAndEmitFirst(Signal cb)
    {
        OnListener += cb;
        return Emit();
    }

    public Task Emit() => signal.Emit();

    public Task EmitAndClear() => signal.EmitAndClear();

    public void Clear() => signal.Clear();

    public async Task Collect(Action action)
    {
        await foreach (var _ in SignalChannel.ReceiveAllAsync())
        {
            action();
        }

        Clear();
    }

    public Task CancelAsync() => Source.CancelAsync();
    public void Cancel() => Source.Cancel();

    public static Listener New() => new();
}

public class Listener<T1>
{
    private readonly HashSet<Signal<T1>> signal = new();
    public event Signal<T1> OnListener
    {
        add { if (value != null) lock (signal) { signal.Add(value); } }
        remove { lock (signal) { signal.Remove(value); } }
    }

    private readonly LazyBox<BufferBlock<T1>> LazyChannel = new();
    private BufferBlock<T1> SignalChannel => LazyChannel.GetOrPut(() =>
    {
        return new BufferBlock<T1>(
            new DataflowBlockOptions
            {
                CancellationToken = Source.Token,
                BoundedCapacity = DataflowBlockOptions.Unbounded
            }).Also(it =>
            {
                Source.Token.Register(() =>
                {
                    it.Complete();
                });
            });
    });

    private readonly LazyBox<CancellationTokenSource> LazySource = new();
    private CancellationTokenSource Source => LazySource.GetOrPut(() => new CancellationTokenSource());

    public Listener<T1> ToFlow()
    {
        OnListener += async (t1, self) =>
        {
            await SignalChannel.SendAsync(t1);

            Source.Token.Register(() =>
            {
                OnListener -= self;
            });
        };

        return this;
    }

    public int Size => signal.Count;

    public Task AddListenerAndEmitFirst(T1 arg1, Signal<T1> cb)
    {
        OnListener += cb;
        return Emit(arg1);
    }

    public Task Emit(T1 arg1) => signal.Emit(arg1);

    public Task EmitAndClear(T1 arg1) => signal.EmitAndClear(arg1);

    public void Clear() => signal.Clear();

    public async Task Collect(Action<T1> action)
    {
        await foreach (var t1 in SignalChannel.ReceiveAllAsync())
        {
            action(t1);
        }

        Clear();
    }

    public Task CancelAsync() => Source.CancelAsync();
    public void Cancel() => Source.Cancel();

    public static Listener<T1> New() => new();
}

public class Listener<T1, T2>
{
    private readonly HashSet<Signal<T1, T2>> signal = new();
    public event Signal<T1, T2> OnListener
    {
        add { if (value != null) lock (signal) { signal.Add(value); } }
        remove { lock (signal) { signal.Remove(value); } }
    }

    private readonly LazyBox<BufferBlock<(T1, T2)>> LazyChannel = new();
    private BufferBlock<(T1, T2)> SignalChannel => LazyChannel.GetOrPut(() =>
    {
        return new BufferBlock<(T1, T2)>(
            new DataflowBlockOptions
            {
                CancellationToken = Source.Token,
                BoundedCapacity = DataflowBlockOptions.Unbounded
            }).Also(it =>
            {
                Source.Token.Register(() =>
                {
                    it.Complete();
                });
            });
    });

    private readonly LazyBox<CancellationTokenSource> LazySource = new();
    private CancellationTokenSource Source => LazySource.GetOrPut(() => new CancellationTokenSource());

    public Listener<T1, T2> ToFlow()
    {
        OnListener += async (t1, t2, self) =>
        {
            await SignalChannel.SendAsync((t1, t2));

            Source.Token.Register(() =>
            {
                OnListener -= self;
            });
        };

        return this;
    }

    public int Size => signal.Count;

    public Task AddListenerAndEmitFirst(T1 arg1, T2 arg2, Signal<T1, T2> cb)
    {
        OnListener += cb;
        return Emit(arg1, arg2);
    }

    public Task Emit(T1 arg1, T2 arg2) => signal.Emit(arg1, arg2);

    public Task EmitAndClear(T1 arg1, T2 arg2) => signal.EmitAndClear(arg1, arg2);

    public void Clear() => signal.Clear();

    public async Task Collect(Action<T1, T2> action)
    {
        await foreach (var (t1, t2) in SignalChannel.ReceiveAllAsync())
        {
            action(t1, t2);
        }

        Clear();
    }

    public Task CancelAsync() => Source.CancelAsync();
    public void Cancel() => Source.Cancel();

    public static Listener<T1, T2> New() => new();
}
