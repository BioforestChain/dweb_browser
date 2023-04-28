namespace DwebBrowser.Helper;

public  static class CollectionExtensions
{

    public static TValue GetValueOrPut<TKey, TValue>(this IDictionary<TKey, TValue> dictionary, TKey key, Func<TValue> putter)
    {
        ArgumentNullException.ThrowIfNull(dictionary, "dictionary");
        if (!dictionary.TryGetValue(key, out var value))
        {
            value = putter();
            dictionary[key] = value;
        }
        return value;
    }

    public static async Task<TValue> GetValueOrPutAsync<TKey, TValue>(this IDictionary<TKey, TValue> dictionary, TKey key, Func<Task<TValue>> putter)
    {
        ArgumentNullException.ThrowIfNull(dictionary, "dictionary");
        if (!dictionary.TryGetValue(key, out var value))
        {
            value = await putter();
            dictionary[key] = value;
        }
        return value;
    }

}

