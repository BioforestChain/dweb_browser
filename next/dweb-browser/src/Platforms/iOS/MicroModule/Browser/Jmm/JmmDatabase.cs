using Foundation;
using System.Text.Json;

namespace DwebBrowser.MicroService.Browser.Jmm;

sealed internal class JmmDatabase : FileStore
{
    internal static JmmDatabase Instance = new("jmm-apps");

    private static int s_id = 0;
    public static readonly State<int> AppMetadataUpdate = new(s_id);
    internal JmmDatabase(string name, StoreOptions options = default) : base(name, options)
    {
    }

    private Dictionary<string, AppMetaData> Apps
    {
        get
        {
            var dic = Get("apps", () => JsonSerializer.Serialize(new Dictionary<string, AppMetaData>()));
            return JsonSerializer.Deserialize<Dictionary<string, AppMetaData>>(dic);
        }
    }

    private void Save()
    {
        Set("apps", JsonSerializer.Serialize(Apps));
    }

    internal bool Upsert(AppMetaData app)
    {
        var oldApp = Apps.GetValueOrDefault(app.Id);

        if (oldApp is not null && oldApp.Equals(app))
        {
            return true;
        }

        //app.Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        Apps.Add(app.Id, app);

        Save();
        return true;
    }

    internal AppMetaData? Find(Mmid mmid)
    {
        return Apps.GetValueOrDefault(mmid);
    }

    internal bool Remove(Mmid mmid)
    {
        if (Apps.ContainsKey(mmid))
        {
            Apps.Remove(mmid);
            Save();
            return true;
        }

        return false;
    }

    internal List<AppMetaData> All() => Apps.Values.OrderByDescending(it => it.Timestamp).ToList();
}
