using System.Collections.Concurrent;

namespace DwebBrowser.Helper;

public record Changes<K, V>(ChangeableMap<K, V> Origin, HashSet<K> Adds, HashSet<K> Updates, HashSet<K> Removes);

public record ChangeState<K>(HashSet<K> Adds, HashSet<K> Updates, HashSet<K> Removes);

public class ChangeableMap<K, V> : ConcurrentDictionary<K, V> where K : notnull
{
    private readonly LazyBox<Changeable<Changes<K, V>>> LazyChangeable = new();
    private Changeable<Changes<K, V>> Changeable =>
        LazyChangeable.GetOrPut(() => new Changeable<Changes<K, V>>(new Changes<K, V>(this, new(), new(), new())));

    public void OnChangeAdd(Signal<Changes<K, V>> cb)
    {
        Changeable.Listener.OnListener += cb;
    }

    public void OnChangeRemove(Signal<Changes<K, V>> cb)
    {
        Changeable.Listener.OnListener -= cb;
    }

    public void OnChangeEmit()
    {
        Changeable.EmitChange();
    }

    public void OnChangeBackgroundEmit(HashSet<K>? adds = null, HashSet<K>? updates = null, HashSet<K>? removes = null)
    {
        adds ??= new();
        updates ??= new();
        removes ??= new();

        Changeable.EmitChangeBackground(new Changes<K, V>(this, adds, updates, removes));
    }

    public new bool TryAdd(K key, V value)
    {
        if (Keys.Contains(key))
        {
            return TryUpdate(key, value, Get(key)).Also(it =>
            {
                if (it)
                {
                    OnChangeBackgroundEmit(updates: new HashSet<K> { key });
                }
            });
        }
        else
        {
            return base.TryAdd(key, value).Also(it =>
            {
                if (it)
                {
                    OnChangeBackgroundEmit(adds: new HashSet<K> { key });
                }
            });
        }
    }

    public bool Set(K key, V value)
    {
        return TryAdd(key, value);
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
        TryRemove(key, out var value);

        return value;
    }

    public bool TryRemove(K key)
    {
        return TryRemove(key, out _);
    }

    public new bool TryRemove(K key, out V value)
    {
        var _bool = base.TryRemove(key, out var baseValue);

        if (_bool)
        {
            value = baseValue;
            OnChangeBackgroundEmit(removes: new HashSet<K> { key });
        }
        else
        {
            value = default;
        }

        return _bool;
    }

    public new void Clear()
    {
        if (Keys.Count > 0)
        {
            var removes = Keys.ToHashSet();
            base.Clear();

            OnChangeBackgroundEmit(removes: removes);
        }
    }

    /// <summary>
    /// 重置 清空所有的事件监听，清空所有的数据
    ///
    /// 注意，这里不会触发任何事件，如果有需要，请使用 clear ，然后再 reset
    /// </summary>
    public void Reset()
    {
        Changeable.Listener.Clear();
        Clear();
    }
}
