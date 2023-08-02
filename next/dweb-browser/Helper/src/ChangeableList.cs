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

    public new Task Clear()
    {
        base.Clear();
        return _OnChangeEmit();
    }

    public new Task AddRange(IEnumerable<T> collections)
    {
        base.AddRange(collections);
        return _OnChangeEmit();
    }

    public new Task Add(T item)
    {
        base.Add(item);
        return _OnChangeEmit();
    }

    public new Task Insert(int index, T item)
    {
        base.Insert(index, item);
        return _OnChangeEmit();
    }

    public new Task InsertRange(int index, IEnumerable<T> collections)
    {
        base.InsertRange(index, collections);
        return _OnChangeEmit();
    }

    public T? LastOrNull()
    {
        return Count == 0 ? default : this[Count - 1];
    }

    public new Task RemoveAt(int index)
    {
        base.RemoveAt(index);
        return _OnChangeEmit();
    }

    public async new Task<int> RemoveAll(Predicate<T> match)
    {
        if (base.RemoveAll(match) is var n && n > 0)
        {
            await _OnChangeEmit();
        }

        return n;
    }

    public async new Task<bool> Remove(T item)
    {
        if (base.Remove(item) is var suc && suc)
        {
            await _OnChangeEmit();
        }

        return suc;
    }

    public new Task RemoveRange(int index, int count)
    {
        base.RemoveRange(index, count);
        return _OnChangeEmit();
    }
}

