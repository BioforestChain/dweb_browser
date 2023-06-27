using DwebBrowser.MicroService.Browser;
using Foundation;
using UIKit;

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

    // 用于外部应用使用universal link调起应用时转为dweb-deeplink
    public override bool OpenUrl(UIApplication application, NSUrl url, NSDictionary options)
    {
        if (url.Scheme is "dweb")
        {
            _ = Task.Run(async () =>
            {
                var deeplink = string.Format("dweb:{0}?{1}", url.Path[1..], url.Query);
                await BrowserNMM.BrowserController.BrowserNMM.NativeFetchAsync(deeplink);
            }).NoThrow();

            return false;
        }

        return base.OpenUrl(application, url, options);
    }
}

