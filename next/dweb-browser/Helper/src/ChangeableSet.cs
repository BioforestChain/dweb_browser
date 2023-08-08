namespace DwebBrowser.Helper;

public class ChangeableSet<E> : HashSet<E>
{
    private readonly LazyBox<Changeable<ChangeableSet<E>>> LazyChangeable = new();
    private Changeable<ChangeableSet<E>> Changeable => LazyChangeable.GetOrPut(() => new Changeable<ChangeableSet<E>>(this));

    public void OnChangeAdd(Signal<ChangeableSet<E>> cb)
    {
        Changeable.Listener.OnListener += cb;
    }

    public void OnChangeRemove(Signal<ChangeableSet<E>> cb)
    {
        Changeable.Listener.OnListener -= cb;
    }

    public void OnChangeEmit()
    {
        Changeable.EmitChange();
    }

    public new bool Add(E element)
    {
        return base.Add(element).Also(it =>
        {
            if (it)
            {
                OnChangeEmit();
            }
        });
    }

    public new void Clear()
    {
        base.Clear();
        OnChangeEmit();
    }

    public new bool Remove(E element)
    {
        return base.Remove(element).Also(it =>
        {
            if (it)
            {
                OnChangeEmit();
            }
        });
    }
}

