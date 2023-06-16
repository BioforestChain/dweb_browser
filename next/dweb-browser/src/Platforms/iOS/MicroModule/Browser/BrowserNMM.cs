using System.Net;
using BrowserFramework;
using DwebBrowser.MicroService.Browser.Jmm;
using DwebBrowser.MicroService.Http;
using Foundation;
using UIKit;
using WebKit;

namespace DwebBrowser.MicroService.Browser;

public class BrowserNMM : IOSNativeMicroModule
{
    static Debugger Console = new("BrowserNMM");
    public BrowserNMM() : base("browser.dweb")
    {
        s_controllerList.Add(new(this));
    }

    public static BrowserWeb webview = new();

    private static readonly List<BrowserController> s_controllerList = new();
    public static BrowserController BrowserController
    {
        get => s_controllerList.FirstOrDefault();
    }

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        await bootstrapContext.Dns.BootstrapAsync("jmm.browser.dweb");

        HttpRouter.AddRoute(IpcMethod.Get, "/openApp", async (request, ipc) =>
        {
            var mmid = request.QueryStringRequired("app_id");
            return BrowserController?.OpenApp(mmid);
        });

        // 关闭App后端
        HttpRouter.AddRoute(IpcMethod.Get, "/closeApp", async (request, ipc) =>
        {
            var mmid = request.QueryStringRequired("app_id");
            return BrowserController?.CloseApp(mmid);
        });

        // App详情
        HttpRouter.AddRoute(IpcMethod.Get, "/detailApp", async (request, ipc) =>
        {
            var mmid = request.QueryStringRequired("app_id");
            var jmmApps = JmmNMM.JmmApps;
            var jsMicroModule = jmmApps.GetValueOrDefault(mmid);

            if (jsMicroModule is not null)
            {
                var data = NSData.FromString(jsMicroModule.Metadata.ToJson(), NSStringEncoding.UTF8);
                var initDownloadStatus = DownloadStatus.Installed;

                var vc = await RootViewController.WaitPromiseAsync();
                await MainThread.InvokeOnMainThreadAsync(async () =>
                {
                    var manager = new DownloadAppManager(data, (nint)initDownloadStatus);

                    manager.DownloadView.Frame = UIScreen.MainScreen.Bounds;
                    JmmNMM.JmmController.View.AddSubview(manager.DownloadView);
                    vc.PushViewController(JmmNMM.JmmController, true);
                });

                return true;
            }

            return new PureResponse(HttpStatusCode.NotFound, Body: new PureUtf8StringBody("not found " + mmid));
        });
    }

    public override async void OpenActivity(Mmid remoteMmid)
    {
        await MainThread.InvokeOnMainThreadAsync(async () =>
        {
            var manager = new BrowserManager();
            //var webview = new BrowserWeb();
            webview.LoadRequest(new NSUrlRequest(new NSUrl("https://dweb.waterbang.top/")));
            //manager.WebViewList = new WKWebView[] { webview };
            manager.ShowWebViewListDataWithList(new WKWebView[] { webview });
            var swiftView = manager.SwiftView;
            swiftView.Frame = UIScreen.MainScreen.Bounds;
            BrowserController.View.AddSubview(swiftView);
            webview.LoadRequest(new NSUrlRequest(new NSUrl("dweb:install?url=https://dweb.waterbang.top/metadata.json")));
        });
    }

    protected override async Task _onActivityAsync(IpcEvent Event, Ipc ipc)
    {
        OpenActivity(ipc.Remote.Mmid);
    }
}

