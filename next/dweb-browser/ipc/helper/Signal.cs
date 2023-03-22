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

	public Signal()
	{
	}

    public bool Off(Func<Args, object?> cb) => ListenerSet.TryRemove(cb).Also(it =>
    {
        if (it)
        {
            CpSet = ListenerSet.ToHashSet();
        }
    });


    public virtual async Task Emit(Args args)
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
    }

    public void Clear() => ListenerSet.Clear();
}

public class SimpleSignal : Signal<byte>
{
    public override Task Emit(byte args)
    {
        return base.Emit(0);
    }
}