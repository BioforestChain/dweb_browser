using DwebBrowserFramework;
using DwebBrowser.MicroService.Browser.Jmm;
using UIKit;

namespace DwebBrowser.MicroService.Browser;

public class BrowserNMM : IOSNativeMicroModule
{
    static readonly Debugger Console = new("BrowserNMM");
    public BrowserNMM() : base("browser.dweb")
    {
        s_controllerList.Add(new(this));
    }

    private static readonly List<BrowserController> s_controllerList = new();
    public static BrowserController BrowserController
    {
        get => s_controllerList.FirstOrDefault();
    }

    record AppInfo(string id, string icon, string name, string short_name);

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        await bootstrapContext.Dns.BootstrapAsync("jmm.browser.dweb");

        HttpRouter.AddRoute(IpcMethod.Get, "/openApp", async (request, ipc) =>
        {
            var mmid = request.QueryStringRequired("app_id");
            return await BrowserController?.OpenJMM(mmid);
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/appsInfo", async (request, ipc) =>
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

        // 关闭App后端
        HttpRouter.AddRoute(IpcMethod.Get, "/closeApp", async (request, ipc) =>
        {
            var mmid = request.QueryStringRequired("app_id");
            return await BrowserController?.CloseJMM(mmid);
        });
    }

    public override async void OpenActivity(Mmid remoteMmid)
    {
        await MainThread.InvokeOnMainThreadAsync(async () =>
        {
            BridgeManager.WebviewGeneratorCallbackWithCallback(configuration =>
            {
                return new BrowserWeb(this, configuration);
            });
            var manager = new BridgeManager();
            var browserView = manager.BrowserView;
            //var webview = new BrowserWeb();
            //webview.LoadRequest(new NSUrlRequest(new NSUrl("https://dweb.waterbang.top/")));
            //manager.WebViewList = new WKWebView[] { webview };
            //manager.ShowWebViewListDataWithList(new WKWebView[] { webview });
            //manager.OpenWebViewUrlWithUrlString("https://dweb.waterbang.top/");
            //manager.OpenWebViewUrlWithUrlString("about:newtab");
            //var swiftView = manager.SwiftView;
            browserView.Frame = UIScreen.MainScreen.Bounds;
            BrowserController.View.AddSubview(browserView);
            //webview.LoadRequest(new NSUrlRequest(new NSUrl("dweb:install?url=https://dweb.waterbang.top/metadata.json")));
        });
    }

    protected override async Task _onActivityAsync(IpcEvent Event, Ipc ipc)
    {
        OpenActivity(ipc.Remote.Mmid);
    }
}

