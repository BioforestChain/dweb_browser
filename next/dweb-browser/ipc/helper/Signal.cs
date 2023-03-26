using System.Collections.Concurrent;
using ConcurrentCollections;
namespace ipc.helper;


enum SIGNAL_CTOR
{
    /**
     * <summary>
     * 返回该值，会解除监听
     * </summary>
     */
    OFF,

    /**
     * <summary>
     * 返回该值，会让接下来的其它监听函数不再触发
     * </summary>
     */
    BREAK
}

public class Signal<Args>
{
    public ConcurrentHashSet<Func<Args, object?>> ListenerSet = new ConcurrentHashSet<Func<Args, object?>>();
    public HashSet<Func<Args, object?>> CpSet = new HashSet<Func<Args, object?>>();

    public Func<bool> Listen(Func<Args, object?> cb)
    {
        // TODO: emit 时的cbs 应该要同步进行修改？
        ListenerSet.Add(cb).Also(it =>
        {
            if (it)
            {
                CpSet = ListenerSet.ToHashSet();
            }
        });

        return () => Off(cb);
    }

    public bool Off(Func<Args, object?> cb) => ListenerSet.TryRemove(cb).Also(it =>
    {
        if (it)
        {
            CpSet = ListenerSet.ToHashSet();
        }
    });


    public Task EmitAsync(Args args) => Task.Run(() =>
        {
            var cbs = CpSet;

            foreach (Func<Args, object?> cb in cbs)
            {
                try
                {
                    if (ListenerSet.Contains(cb))
                    {
                        continue;
                    }

                    switch (cb(args))
                    {
                        case SIGNAL_CTOR.OFF:
                            ListenerSet.TryRemove(cb);
                            break;
                        case SIGNAL_CTOR.BREAK:
                            break;
                    }
                }
                catch (Exception e)
                {
                    Console.WriteLine(e.StackTrace);
                }
            }
        });

    public void Clear() => ListenerSet.Clear();
}

public class SimpleSignal : Signal<byte>
{
    public Task EmitAsync() => base.EmitAsync(0);
}