using UIKit;
using WebKit;
using Foundation;
using CoreGraphics;
using BrowserFramework;

namespace DwebBrowser.MicroService.Browser;

public class BrowserNMM : IOSNativeMicroModule
{
    static Debugger Console = new("BrowserNMM");
    public BrowserNMM() : base("browser.dweb")
    {
        s_controllerList.Add(new(this));
    }

    private static readonly List<BrowserController> s_controllerList = new();
    public static BrowserController BrowserController
    {
        get => s_controllerList.FirstOrDefault();
    }

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        HttpRouter.AddRoute(IpcMethod.Get, "/openApp", async (request, ipc) =>
        {
            var mmid = request.QueryStringRequired("app_id");
            return BrowserController?.OpenApp(mmid);
        });
    }

    public override async void OpenActivity(Mmid remoteMmid)
    {
        var vc = await RootViewController.WaitPromiseAsync();

        await MainThread.InvokeOnMainThreadAsync(async () =>
        {
            var manager = new BrowserManager();
            var webview = new WKWebView(CGRect.Empty, new());
            webview.LoadRequest(new NSUrlRequest(new NSUrl("https://www.baidu.com")));
            manager.WebViewList = new WKWebView[] { webview };
            var swiftView = manager.SwiftView;
            swiftView.Frame = UIScreen.MainScreen.Bounds;
            BrowserController.View.AddSubview(swiftView);
        });
    }

    protected override async Task _onActivityAsync(IpcEvent Event, Ipc ipc) 
    {
        OpenActivity(ipc.Remote.Mmid);
    }
}

