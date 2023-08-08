using System.Collections.Concurrent;

namespace DwebBrowser.Helper;

public class ChangeableMap<K, V> : ConcurrentDictionary<K, V> where K : notnull
{
    private readonly LazyBox<Changeable<ChangeableMap<K, V>>> LazyChangeable = new();
    private Changeable<ChangeableMap<K, V>> Changeable =>
        LazyChangeable.GetOrPut(() => new Changeable<ChangeableMap<K, V>>(this));

    public void OnChangeAdd(Signal<ChangeableMap<K, V>> cb)
    {
        Changeable.Listener.OnListener += cb;
    }

    public void OnChangeRemove(Signal<ChangeableMap<K, V>> cb)
    {
        Changeable.Listener.OnListener -= cb;
    }

    public void OnChangeEmit()
    {
        Changeable.EmitChange();
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
