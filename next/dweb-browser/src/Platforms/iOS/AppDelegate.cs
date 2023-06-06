using UIKit;
using WebKit;
using Foundation;
using CoreGraphics;
using BrowserFramework;
using DwebBrowser.MicroService.Browser;

namespace DwebBrowser.Platforms.iOS;

[Register("AppDelegate")]
public class AppDelegate : MauiUIApplicationDelegate
{
    protected override MauiApp CreateMauiApp() => MauiProgram.CreateMauiApp();

    public override bool FinishedLaunching(UIApplication application, NSDictionary launchOptions)
    {
        _ = base.FinishedLaunching(application, launchOptions);
        // create a new window instance based on the screen size
        Window = application.KeyWindow;

        // create a UIViewController with a single UILabel
        var nav = new UINavigationController(BrowserNMM.BrowserController);
        nav.SetNavigationBarHidden(true, false);

        Window.RootViewController = nav;
        // 保存到全局
        IOSNativeMicroModule.Window.Resolve(Window);
        IOSNativeMicroModule.RootViewController.Resolve(nav);

        // make the window visible
        Window.MakeKeyAndVisible();

        return true;
    }
}

