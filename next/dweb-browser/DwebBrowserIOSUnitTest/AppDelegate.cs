using DwebBrowser.MicroService.Browser;
using DwebBrowser.MicroService.Browser.Jmm;
using DwebBrowser.MicroService.Browser.JsProcess;
//using System.Diagnostics;
using System.Net.Http;
using DwebBrowserIOSUnitTest.Tests;
using DwebBrowser.Helper;

namespace DwebBrowserIOSUnitTest;

[Register("AppDelegate")]
public class AppDelegate : UIApplicationDelegate
{
    public override UIWindow? Window
    {
        get;
        set;
    }

    public override bool FinishedLaunching(UIApplication application, NSDictionary launchOptions)
    {
        // create a new window instance based on the screen size
        Window = new UIWindow(UIScreen.MainScreen.Bounds);

        //ColorTest.FromRgba_string_ReturnSuccess();
        //UTTypesTest.UTTypes_ToString();

        Debugger.DebugTags = new() { "*" };

        var browserNMM = new BrowserNMM();
        var browser = new BrowserWeb();
        browser.LoadRequest(new NSUrlRequest(new NSUrl("https://dweb.waterbang.top/")));
        browser.Frame = UIScreen.MainScreen.Bounds;
        // create a UIViewController with a single UILabel
        var vc = new UIViewController();
        vc.View!.AddSubview(browser);
        vc.View.Frame = UIScreen.MainScreen.Bounds;
        Window.RootViewController = vc;

        // make the window visible
        Window.MakeKeyAndVisible();

        return true;
    }
}

