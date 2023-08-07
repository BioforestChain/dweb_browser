namespace DwebBrowser.Helper;

public class ChangeableList<T> : List<T>
{
    private readonly HashSet<Signal<ChangeableList<T>>> _changeSignal = new();
    public event Signal<ChangeableList<T>> OnChange
    {
        add { if (value != null) lock (_changeSignal) { _changeSignal.Add(value); } }
        remove { lock (_changeSignal) { _changeSignal.Remove(value); } }
    }
    protected Task _OnChangeEmit() => _changeSignal.Emit(this).ForAwait();

    public void OnChangeEmit()
    {
        _ = Task.Run(_OnChangeEmit).NoThrow();
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

