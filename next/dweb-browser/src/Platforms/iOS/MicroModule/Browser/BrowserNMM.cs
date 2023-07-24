using DwebBrowserFramework;
using UIKit;

namespace DwebBrowser.MicroService.Browser;

public class BrowserNMM : IOSNativeMicroModule
{
    static readonly Debugger Console = new("BrowserNMM");
    public BrowserNMM() : base("web.browser.dweb")
    {
        s_controllerList.Add(new(this));
    }

    private static readonly List<BrowserController> s_controllerList = new();
    public static BrowserController BrowserController
    {
        get => s_controllerList.FirstOrDefault();
    }

    public override List<Dweb_DeepLink> Dweb_deeplinks { get; init; } = new() { "dweb:search" };



    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        HttpRouter.AddRoute(IpcMethod.Get, "/search", async (request, ipc) =>
        {
            // TODO: 触发Browser搜索

            return null;
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

