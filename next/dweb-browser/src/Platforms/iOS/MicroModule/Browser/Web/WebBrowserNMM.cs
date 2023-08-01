using DwebBrowserFramework;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Web;

public class WebBrowserNMM : IOSNativeMicroModule
{
    static readonly Debugger Console = new("WebNMM");
    public override string Name { get; set; } = "Web Browser";
    public override string ShortName { get; set; } = "Browser";
    public WebBrowserNMM() : base("web.browser.dweb")
    {
        s_controllerList.Add(new(this));
    }

    private static readonly List<WebBrowserController> s_controllerList = new();
    public static WebBrowserController WebBrowserController
    {
        get => s_controllerList.FirstOrDefault();
    }

    public override List<MicroModuleCategory> Categories { get; init; } = new()
    {
        MicroModuleCategory.Application,
        MicroModuleCategory.Web_Browser,
    };

    public override List<Core.ImageSource> Icons { get { return new() { new Core.ImageSource("file:///sys/browser/web/logo.svg") }; } }

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        
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
            browserView.Frame = UIScreen.MainScreen.Bounds;
            WebBrowserController.View.AddSubview(browserView);

            var vc = await RootViewController.WaitPromiseAsync();
            vc.PushViewController(WebBrowserController, true);
        });
    }

    protected override async Task _onActivityAsync(IpcEvent Event, Ipc ipc)
    {
        OpenActivity(Mmid);
    }
}

