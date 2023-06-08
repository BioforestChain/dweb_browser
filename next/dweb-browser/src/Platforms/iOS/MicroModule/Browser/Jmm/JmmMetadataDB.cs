using System.Linq;
using Foundation;
using System.Runtime.InteropServices;

namespace DwebBrowser.MicroService.Browser.Jmm;

public static class JmmMetadataDB
{
    public static readonly string PREFERENCE_NAME = "JmmMetadata";

    public static JmmMetadata QueryJmmMetadata(string key, JmmMetadata defaultValue = default) =>
        GetJmmMetadataEnumerator().ToList().Find(entry => entry.Key == key).Value ?? defaultValue;

    public static IEnumerable<KeyValuePair<Mmid, JmmMetadata>> GetJmmMetadataEnumerator()
    {
        var nsDic = NSUserDefaults.StandardUserDefaults.DictionaryForKey(PREFERENCE_NAME);
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

    public static void SaveJmmMetadata(Mmid mmid, JmmMetadata jmmMetadata)
    {
        var nsDictionary = new NSMutableDictionary<NSString, NSString>();
        foreach (var entry in GetJmmMetadataEnumerator())
        {
            nsDictionary.Add(new NSString(entry.Key), new NSString(entry.Value.ToJson()));
        }

        nsDictionary.Add(new NSString(mmid), new NSString(jmmMetadata.ToJson()));   

        NSUserDefaults.StandardUserDefaults.SetValueForKey(nsDictionary, new NSString(PREFERENCE_NAME));
    }
}

