using System.Text.Json;

namespace DwebBrowser.MicroService.Browser.Desk;

public partial class DeskController
{
    public DWebView.DWebView DesktopView { get; set; }

    /// <summary>
    /// 列出桌面应用
    /// </summary>
    /// <returns></returns>
    public async Task<List<DeskNMM.DesktopAppMetadata>> GetDesktopAppList()
    {
        var apps = await DeskNMM.BootstrapContext.Dns.Search(MicroModuleCategory.Application);

        List<DeskNMM.DesktopAppMetadata> desktopApps = new();
        foreach (var app in apps)
        {
            var deskApp = DeskNMM.DesktopAppMetadata.FromMicroModuleManifest(app);
            deskApp.Running = DeskNMM.RunningApps.ContainsKey(app.Mmid);
            desktopApps.Add(deskApp);
        }

        return desktopApps.OrderBy(it => DeskAppsStore.Instance.AppOrders.GetValueOrDefault(it.Mmid)?.Order ?? 0).ToList();
    }
}

