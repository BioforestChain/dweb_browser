using System.Threading.Tasks.Dataflow;

namespace DwebBrowser.Helper;

public record Child<Args, R>
(
    Listener<Args> ParentListener,
    Listener<R> ChildListener,
    Func<Args, bool> Filter,
    Func<Args, R> Map
);

public class Listener
{
    static readonly Debugger Console = new("Listener");
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

    public async Task Collect(Func<Task> action)
    {
        await foreach (var _ in SignalChannel.ReceiveAllAsync())
        {
            await action();
        }

        Clear();
    }

    public Task CancelAsync() => Source.CancelAsync();
    public void Cancel() => Source.Cancel();

    public static Listener New() => new();

    public static Listener CollectEmitListener()
    {
        var listener = New();

        _ = listener.Collect(() =>
        {
            listener.Emit();
        }).NoThrow();

        return listener;
    }
}

public class Listener<T1>
{
    static readonly Debugger Console = new("Listener");
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

    public async Task Emit(T1 arg1)
    {
        await signal.Emit(arg1);

        var childList = CopyChildrenToList();

        foreach (var child in childList)
        {
            try
            {
                if (child.Filter(arg1))
                {
                    var childArgs = child.Map(arg1);
                    await child.ChildListener.Emit(childArgs);
                }
            }
            catch (Exception e)
            {
                Console.Error("Emit", e.Message);
            }
        }
    }

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

    public async Task Collect(Func<T1, Task> action)
    {
        await foreach (var t1 in SignalChannel.ReceiveAllAsync())
        {
            await action(t1);
        }

        Clear();
    }

    public Task CancelAsync() => Source.CancelAsync();
    public void Cancel() => Source.Cancel();

    public static Listener<T1> New() => new();

    public static Listener<T1> CollectEmitListener()
    {
        var listener = New();

        _ = listener.Collect((arg1) =>
        {
            return listener.Emit(arg1).NoThrow();
        }).NoThrow();

        return listener;
    }

    private readonly Dictionary<Listener<dynamic>, Child<T1, dynamic>> Children = new();
    public Listener<dynamic> CreateChild(Func<T1, bool> filter, Func<T1, dynamic> map)
    {
        var child = new Child<T1, dynamic>(this, Listener<dynamic>.New(), filter, map);

        lock (Children)
        {
            Children.Add(child.ChildListener, child);
        }

        return child.ChildListener;
    }

    public bool RemoveChild(Listener<dynamic> listener)
    {
        lock (Children)
        {
            return Children.Remove(listener);
        }
    }

    private List<Child<T1, dynamic>> CopyChildrenToList()
    {
        lock (Children)
        {
            return Children.Values.ToList();
        }
    }
}

public class Listener<T1, T2>
{
    static readonly Debugger Console = new("Listener");
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

    public async Task Collect(Func<T1, T2, Task> action)
    {
        await foreach (var (t1, t2) in SignalChannel.ReceiveAllAsync())
        {
            await action(t1, t2);
        }

        Clear();
    }

    public Task CancelAsync() => Source.CancelAsync();
    public void Cancel() => Source.Cancel();

    public static Listener<T1, T2> New() => new();

    public static Listener<T1, T2> CollectEmitListener()
    {
        var listener = New();

        _ = listener.Collect((arg1, arg2) =>
        {
            listener.Emit(arg1, arg2);
        }).NoThrow();

        return listener;
    }
}
