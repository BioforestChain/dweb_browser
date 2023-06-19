using Foundation;

namespace DwebBrowser.MicroService.Browser.Jmm;

public static class JmmMetadataDB
{
    public static readonly string PREFERENCE_NAME = "JmmMetadata";

    private static int s_id = 0;
    public static readonly State<int> JmmMetadataUpdate = new(s_id);
    private static NSUserDefaults _userDefaults = NSUserDefaults.StandardUserDefaults;

    static JmmMetadataDB()
    {
        var nsDic = _userDefaults.DictionaryForKey(PREFERENCE_NAME);

        if (nsDic is null)
        {
            nsDic = new NSMutableDictionary<NSString, NSString>();
            _userDefaults.SetValueForKey(nsDic, new NSString(PREFERENCE_NAME));
        }
        //_userDefaults.RegisterDefaults();
    }

    public static JmmMetadata QueryJmmMetadata(Mmid key, JmmMetadata defaultValue = default) =>
        GetJmmMetadataEnumerator().ToList().Find(entry => entry.Key == key).Value ?? defaultValue;

    public static IEnumerable<KeyValuePair<Mmid, JmmMetadata>> GetJmmMetadataEnumerator()
    {
        var nsDic = _userDefaults.DictionaryForKey(PREFERENCE_NAME);
        var dic = new Dictionary<Mmid, JmmMetadata>();

        foreach (NSString key in nsDic.Keys)
        {
            var value = nsDic[key];
            if (value is NSString)
            {
                try
                {
                    JmmMetadata metadata = JmmMetadata.FromJson(value as NSString);
                    dic.Add((key as NSString).ToString(), metadata);
                }
                catch { }
            }
        }

        foreach (var entry in dic)
        {
            yield return entry;
        }
    }

    public static IEnumerable<KeyValuePair<NSString, NSString>> GetJmmMetadataJsonEnumerator()
    {
        var nsDic = _userDefaults.DictionaryForKey(PREFERENCE_NAME);

        foreach (var entry in nsDic)
        {
            yield return KeyValuePair.Create((NSString)entry.Key, (NSString)entry.Value);
        }
    }

    public static void AddJmmMetadata(Mmid mmid, JmmMetadata jmmMetadata)
    {
        var nsDictionary = new NSMutableDictionary<NSString, NSString>();

        foreach (var entry in GetJmmMetadataEnumerator())
        {
            nsDictionary.Add(new NSString(entry.Key), new NSString(entry.Value.ToJson()));
        }

        nsDictionary.Add(new NSString(mmid), new NSString(jmmMetadata.ToJson()));

        _userDefaults.SetValueForKey(nsDictionary, new NSString(PREFERENCE_NAME));
        JmmMetadataUpdate.Set(Interlocked.Increment(ref s_id));
    }

    public static void AddJmmMetadata(Dictionary<Mmid, JsMicroModule> apps)
    {
        var nsDictionary = new NSMutableDictionary<NSString, NSString>();

        foreach (var entry in apps)
        {
            nsDictionary.Add(new NSString(entry.Key), new NSString(entry.Value.Metadata.ToJson()));
        }

        _userDefaults.SetValueForKey(nsDictionary, new NSString(PREFERENCE_NAME));
        JmmMetadataUpdate.Set(Interlocked.Increment(ref s_id));
    }

    public static void AddJmmMetadata(Dictionary<Mmid, JmmMetadata> apps)
    {
        var nsDictionary = new NSMutableDictionary<NSString, NSString>();

        foreach (var entry in apps)
        {
            nsDictionary.Add(new NSString(entry.Key), new NSString(entry.Value.ToJson()));
        }

        _userDefaults.SetValueForKey(nsDictionary, new NSString(PREFERENCE_NAME));
        JmmMetadataUpdate.Set(Interlocked.Increment(ref s_id));
    }

    public static void RemoveJmmMetadata(Mmid mmid)
    {
        var _mmid = new NSString(mmid);
        var nsDictionary = new NSMutableDictionary<NSString, NSString>();

        foreach (var entry in GetJmmMetadataJsonEnumerator())
        {
            if (entry.Key != _mmid)
            {
                nsDictionary.Add(entry.Key, entry.Value);
            }
        }

        _userDefaults.SetValueForKey(nsDictionary, new NSString(PREFERENCE_NAME));
        JmmMetadataUpdate.Set(Interlocked.Decrement(ref s_id));
    }
}

