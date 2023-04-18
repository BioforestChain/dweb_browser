using System.Collections.Generic;
using Foundation;

namespace DwebBrowser.WebModule.Jmm;

// TODO: 简单的存储JmmMetadata，待优化
public static class JmmMetadataDB
{
    public static JmmMetadata QueryJmmMetadata(string key, JmmMetadata defaultValue = default) =>
        JmmMetadata.FromJson(NSUserDefaults.StandardUserDefaults.StringForKey(key)) ?? defaultValue;


    public static List<KeyValuePair<Mmid, JmmMetadata>> QueryJmmMetadataList() =>
        NSUserDefaults.StandardUserDefaults.ToDictionary()
            .ToDictionary(
                k => (k.Key as NSString).ToString() as Mmid,
                v => JmmMetadata.FromJson(v.Value as NSString))
        .ToList();


    public static void SaveJmmMetadata(Mmid mmid, JmmMetadata jmmMetadata) =>
        NSUserDefaults.StandardUserDefaults.SetString(jmmMetadata.ToJson(), mmid);

}

