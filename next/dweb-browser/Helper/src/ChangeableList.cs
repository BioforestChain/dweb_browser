namespace DwebBrowser.Helper;

public class ChangeableList<T> : List<T>
{
    private readonly LazyBox<Changeable<ChangeableList<T>>> LazyChangeable = new();
    private Changeable<ChangeableList<T>> Changeable => LazyChangeable.GetOrPut(() => new Changeable<ChangeableList<T>>(this));

    public void OnChangeAdd(Signal<ChangeableList<T>> cb)
    {
        Changeable.Listener.OnListener += cb;
    }

    public void OnChangeRemove(Signal<ChangeableList<T>> cb)
    {
        Changeable.Listener.OnListener -= cb;
    }

    public void OnChangeEmit()
    {
        Changeable.EmitChange();
    }

    public new void Clear()
    {
        base.Clear();
        OnChangeEmit();
    }

    public new void AddRange(IEnumerable<T> collections)
    {
        base.AddRange(collections);
        OnChangeEmit();
    }

    public new void Add(T item)
    {
        base.Add(item);
        OnChangeEmit();
    }

    public new void Insert(int index, T item)
    {
        base.Insert(index, item);
        OnChangeEmit();
    }

    public new void InsertRange(int index, IEnumerable<T> collections)
    {
        base.InsertRange(index, collections);
        OnChangeEmit();
    }

    public T? LastOrNull()
    {
        return Count == 0 ? default : this[Count - 1];
    }

    public new void RemoveAt(int index)
    {
        base.RemoveAt(index);
        OnChangeEmit();
    }

    public new int RemoveAll(Predicate<T> match)
    {
        if (base.RemoveAll(match) is var n && n > 0)
        {
            OnChangeEmit();
        }

        return n;
    }

    public new bool Remove(T item)
    {
        if (base.Remove(item) is var suc && suc)
        {
            OnChangeEmit();
        }

        return suc;
    }

    public new void RemoveRange(int index, int count)
    {
        base.RemoveRange(index, count);
        OnChangeEmit();
    }
}

