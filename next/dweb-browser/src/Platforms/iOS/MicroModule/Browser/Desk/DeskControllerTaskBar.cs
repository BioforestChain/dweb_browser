using CoreGraphics;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Desk;

public partial class DeskController
{
    /// <summary>
    /// 右侧 TaskBar
    /// </summary>
    public UIView TaskBarView { get; set; }

    public record TaskBarRect(int width, int height);
    public TaskBarRect TaskBarResize(int width, int height)
    {
        var display = UIScreen.MainScreen.Bounds;
        var uGap = width / 5;
        var x = display.Width - width - uGap;
        var y = (display.Height - height) / 2;

        TaskBarView.Frame = new CGRect(Math.Round(x), Math.Round(y), width, height);

        return new TaskBarRect(width, height);
    }

    public List<TaskAppsStore.TaskApps> TaskBarAppList = TaskAppsStore.Instance.All();
    public async Task<List<DeskNMM.DesktopAppMetadata>> GetTaskbarAppList(int limit)
    {
        List<DeskNMM.DesktopAppMetadata> apps = new();

        foreach (var app in TaskBarAppList)
        {
            var metadata = await DeskNMM.BootstrapContext.Dns.Query(app.Mmid);
            if (metadata is not null)
            {
                var deskApp = DeskNMM.DesktopAppMetadata.FromMicroModuleManifest(metadata);
                deskApp.Running = DeskNMM.RunningApps.ContainsKey(metadata.Mmid);

                apps.Add(deskApp);
            }

            if (apps.Count >= limit)
            {
                return apps;
            }
        }

        return apps;
    }

    public URL GetTaskbarUrl() => new URL(TaskbarServer.StartResult.urlInfo.BuildInternalUrl()
        .Path("/taskbar.html")).SearchParamsSet("api-base", TaskbarServer.StartResult.urlInfo.BuildPublicUrl().ToString());
}

