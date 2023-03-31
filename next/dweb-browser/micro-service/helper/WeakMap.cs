using System.Diagnostics.CodeAnalysis;

namespace micro_service;

/**
 * 暂时没有找到 C# 中 WeakMap 的替代品，这里临时实现了一个。但因为Key是强引用，所以还是最好别用。
 */
public class WeakMap<TKey, TValue>
where TKey : notnull
 where TValue : class
{
    private readonly Dictionary<TKey, WeakReference<TValue>> _map = new Dictionary<TKey, WeakReference<TValue>>();

    public void Add(TKey key, TValue value)
    {
        WeakReference<TValue> weakRef = new WeakReference<TValue>(value);
        _map[key] = new WeakReference<TValue>(value);
    }

    public bool TryGet(TKey key, [MaybeNullWhen(false)] out TValue value)
    {
        WeakReference<TValue> weakRef;
        if (_map.TryGetValue(key, out weakRef))
        {
            if (weakRef.TryGetTarget(out value))
            {
                return true;
            }
        }
        // If the key is no longer referenced, remove it from the map
        _map.Remove(key);
        value = default(TValue);
        return false;
    }
    public TValue? TryGet(TKey key)
    {
        TValue value;
        if (TryGet(key, out value))
        {
            return value;
        }
        return null;
    }
}
