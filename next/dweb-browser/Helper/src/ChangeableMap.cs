using System.Collections.Concurrent;

namespace DwebBrowser.Helper;

public class ChangeableMap<K, V> : ConcurrentDictionary<K, V> where K : notnull
{
    private readonly HashSet<Signal<ConcurrentDictionary<K, V>>> _changeSignal = new();
    public event Signal<ConcurrentDictionary<K, V>> OnChange
    {
        add { if (value != null) lock (_changeSignal) { _changeSignal.Add(value); } }
        remove { lock (_changeSignal) { _changeSignal.Remove(value); } }
    }
    protected Task _OnChangeEmit() => _changeSignal.Emit(this).ForAwait();

    public void OnChangeEmit()
    {
        _ = Task.Run(_OnChangeEmit).NoThrow();
    }

    public bool Set(K key, V value)
    {
        return TryAdd(key, value).Also(it =>
        {
            if (it)
            {
                OnChangeEmit();
            }
        });
    }

    public V? Get(K key)
    {
        if (TryGetValue(key, out var value))
        {
            return value;
        }

        return default;
    }

    public V? Remove(K key)
    {
        if (TryRemove(key, out var value))
        {
            OnChangeEmit();
            return value;
        }
        
        return default;
    }

    public new void Clear()
    {
        base.Clear();
        OnChangeEmit();
    }
}
