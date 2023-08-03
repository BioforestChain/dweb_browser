using System.Text.Json;

namespace DwebBrowser.MicroService.Browser.Jmm;

sealed internal class JmmDatabase: FileStore<Dictionary<Mmid, JmmAppInstallManifest>, KeyValuePair<Mmid, JmmAppInstallManifest>>
{
    internal static JmmDatabase Instance = new("jmm-apps");

    internal JmmDatabase(string name, StoreOptions options = default) : base(name, options)
    {
        Apps = Get("apps", () => new Dictionary<Mmid, JmmAppInstallManifest>());
    }

    private Dictionary<Mmid, JmmAppInstallManifest> Apps { get; init; }

    private void Save()
    {
        Set("apps", Apps);
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
