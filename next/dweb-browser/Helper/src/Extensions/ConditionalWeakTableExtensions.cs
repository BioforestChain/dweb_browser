using System.Runtime.CompilerServices;

namespace DwebBrowser.Helper;

public static class ConditionalWeakTableExtensions
{
    public static TValue GetValueOrPut<TKey, TValue>(this ConditionalWeakTable<TKey, TValue> weakDictionary, TKey key, Func<TValue> putter)
        where TKey: class where TValue: class
    {
        ArgumentNullException.ThrowIfNull(weakDictionary, "weakDictionary");
        if (!weakDictionary.TryGetValue(key, out var value))
        {
            value = putter();
            weakDictionary.Add(key, value);
        }
        return value;
    }
}

