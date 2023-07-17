using DwebBrowser.MicroService.Browser.Jmm;

namespace DwebBrowser.MicroService.Browser.Desktop;

public class DesktopNMM : NativeMicroModule
{
    static readonly Debugger Console = new("DesktopNMM");
    public DesktopNMM(): base("desktop.browser.dweb")
    {
    }

    record AppInfo(string id, string icon, string name, string short_name);

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        HttpRouter.AddRoute(IpcMethod.Get, "/appsInfo", async (_, _) =>
        {
            var apps = JmmNMM.JmmApps;
            Console.Log("appInfo", "size: {0}", apps.Count);
            var responseApps = new List<AppInfo> { };

            foreach (var app in apps)
            {
                var metadata = app.Value.Metadata;
                responseApps.Add(new AppInfo(metadata.Id, metadata.Icon, metadata.Name, metadata.ShortName));
            }

            return responseApps;
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/openAppOrAcitvite", async (request, _) =>
        {
            var mmid = request.QueryStringRequired("app_id");

            await JmmNMM.JmmController.OpenApp(mmid);

            return true;
        });
    }

    
}

