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
    protected Task _OnChangeEmit(ConcurrentDictionary<K, V> dic) => _changeSignal.Emit(dic).ForAwait();

    public Task OnChangeEmit() => _OnChangeEmit(this);

    public async Task<bool> Set(K key, V value)
    {
        var suc = TryAdd(key, value);
        await _OnChangeEmit(this);
        return suc;
    }

    public V? Get(K key)
    {
        if (TryGetValue(key, out var value))
        {
            return value;
        }

        return default;
    }

    public async Task<V?> Remove(K key)
    {
        if (TryRemove(key, out var value))
        {
            await _OnChangeEmit(this);
            return value;
        }
        
        return default;
    }
}
