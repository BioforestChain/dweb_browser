using DwebBrowser.MicroService.Sys.Jmm;
using DwebBrowser.MicroService.Sys.Js;
using System.Diagnostics;
using System.Net.Http;
using DwebBrowserIOSUnitTest.Tests;

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
        UTTypesTest.UTTypes_ToString();


        // create a UIViewController with a single UILabel
        var vc = new UIViewController();
        vc.View!.AddSubview(new UILabel(Window!.Frame)
        {
            BackgroundColor = UIColor.SystemBackground,
            TextAlignment = UITextAlignment.Center,
            Text = "Hello, iOS!",
            AutoresizingMask = UIViewAutoresizing.All,
        });
        Window.RootViewController = vc;

        // make the window visible
        Window.MakeKeyAndVisible();

        return true;
    }
}

