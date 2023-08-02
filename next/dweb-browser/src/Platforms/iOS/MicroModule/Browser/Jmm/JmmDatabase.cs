using System.Text.Json;

namespace DwebBrowser.MicroService.Browser.Jmm;

sealed internal class JmmDatabase : FileStore
{
    internal static JmmDatabase Instance = new("jmm-apps");

    internal JmmDatabase(string name, StoreOptions options = default) : base(name, options)
    {
    }

    private readonly LazyBox<Dictionary<Mmid, JmmAppInstallManifest>> SApps = new();
    private Dictionary<Mmid, JmmAppInstallManifest> Apps
    {
        get
        {
            /// TODO: 待优化
            var dic = JsonSerializer.Deserialize<Dictionary<Mmid, JmmAppInstallManifest>>(Get("apps",
                () => JsonSerializer.Serialize(new Dictionary<Mmid, JmmAppInstallManifest>())));

            if (dic.Count == 0)
            {
                return SApps.GetOrPut(() => new Dictionary<string, JmmAppInstallManifest>());
            }

            return dic;
        }
    }

    private void Save()
    {
        Set("apps", JsonSerializer.Serialize(Apps));
    }

    internal bool Upsert(JmmAppInstallManifest app)
    {
        var oldApp = Apps.GetValueOrDefault(app.Id);

        if (oldApp is not null && oldApp.Equals(app))
        {
            return true;
        }

        Apps.Add(app.Id, app);

        Save();
        return true;
    }

    internal JmmAppInstallManifest? Find(Mmid mmid)
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

    internal List<JmmAppInstallManifest> All() => Apps.Values.ToList();
}
