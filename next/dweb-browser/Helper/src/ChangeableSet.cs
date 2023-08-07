namespace DwebBrowser.Helper;

public class ChangeableSet<E> : HashSet<E>
{
    private readonly HashSet<Signal<ChangeableSet<E>>> _changeSignal = new();
    public event Signal<ChangeableSet<E>> OnChange
    {
        add { if (value != null) lock (_changeSignal) { _changeSignal.Add(value); } }
        remove { lock (_changeSignal) { _changeSignal.Remove(value); } }
    }
    protected Task _OnChangeEmit() => _changeSignal.Emit(this).ForAwait();

    public void OnChangeEmit()
    {
        _ = Task.Run(_OnChangeEmit).NoThrow();
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

