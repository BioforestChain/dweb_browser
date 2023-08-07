using DwebBrowser.Helper;
using Foundation;
using UIKit;

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
        // create a UIViewController with a single UILabel
        var vc = new UIViewController();
        //vc.View.Frame = UIScreen.MainScreen.Bounds;
        Window.RootViewController = vc;

        // make the window visible
        Window.MakeKeyAndVisible();

        return true;
    }
}

