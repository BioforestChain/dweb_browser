using System.Collections.Concurrent;

namespace DwebBrowser.Helper;

public class ChangeableMap<K, V> where K : notnull, IComparable
{
    private readonly ConcurrentDictionary<K, V> InnerMap = new();
    private readonly HashSet<Signal<ConcurrentDictionary<K, V>>> _changeSignal = new();
    public event Signal<ConcurrentDictionary<K, V>> OnChange
    {
        add { if (value != null) lock (_changeSignal) { _changeSignal.Add(value); } }
        remove { lock (_changeSignal) { _changeSignal.Remove(value); } }
    }
    protected Task _OnChangeEmit(ConcurrentDictionary<K, V> dic) => _changeSignal.Emit(dic).ForAwait();

    public Task OnChangeEmit() => _OnChangeEmit(InnerMap);

    public int Length => InnerMap.Count;

    public async Task<bool> Set(K key, V value)
    {
        var suc = InnerMap.TryAdd(key, value);
        await _OnChangeEmit(InnerMap);
        return suc;
    }

    public V? Get(K key)
    {
        return InnerMap.GetValueOrDefault(key);
    }

    public async Task<V?> Remove(K key)
    {
        InnerMap.Remove(key, out var res);
        await _OnChangeEmit(InnerMap);
        return res;
    }

    public bool ContainsKey(K key)
    {
        return InnerMap.ContainsKey(key);
    }

    public bool Contains(KeyValuePair<K, V> value)
    {
        return InnerMap.Contains(value);
    }

    public void Clear()
    {
        InnerMap.Clear();
    }
}

