using Foundation;
using System.Text.Json;

namespace DwebBrowser.MicroService.Browser.Jmm;

sealed internal class JmmDatabase : FileStore
{
    internal static JmmDatabase Instance = new("jmm-apps");

    internal JmmDatabase(string name, StoreOptions options = default) : base(name, options)
    {
    }

    private Dictionary<Mmid, IJmmAppInstallManifest> Apps
    {
        get
        {
            var dic = Get("apps", () => JsonSerializer.Serialize(new Dictionary<Mmid, IJmmAppInstallManifest>()));
            return JsonSerializer.Deserialize<Dictionary<Mmid, IJmmAppInstallManifest>>(dic);
        }
    }

    private void Save()
    {
        Set("apps", JsonSerializer.Serialize(Apps));
    }

    internal bool Upsert(IJmmAppInstallManifest app)
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

    internal IJmmAppInstallManifest? Find(Mmid mmid)
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

    internal List<IJmmAppInstallManifest> All() => Apps.Values.ToList();
}
