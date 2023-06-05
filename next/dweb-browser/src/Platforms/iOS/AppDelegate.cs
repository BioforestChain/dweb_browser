using UIKit;
using WebKit;
using Foundation;
using CoreGraphics;
using BrowserFramework;

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
        var vc = new MyViewController();
        var nav = new UINavigationController(vc);
        nav.SetNavigationBarHidden(true, false);

        Window.RootViewController = nav;
        // 保存到全局
        IOSNativeMicroModule.Window.Resolve(Window);
        IOSNativeMicroModule.RootViewController.Resolve(nav);

        // make the window visible
        Window.MakeKeyAndVisible();

        return true;
    }

    /// <summary>
    /// 缺省的启动屏幕
    /// </summary>
    public class MyViewController : UIViewController
    {
        public override void ViewDidLoad()
        {
            base.ViewDidLoad();

            var manager = new BrowserManager();
            var webview = new WKWebView(new CGRect(0, 100, 100, 100), new());
            webview.LoadRequest(new NSUrlRequest(new NSUrl("https://www.baidu.com")));
            manager.WebViewList = new WKWebView[] { webview };
            var swiftView = manager.SwiftView;

            swiftView.Frame = UIScreen.MainScreen.Bounds;
            //View = manager.SwiftView;
            View.AddSubview(swiftView);
        }
    }

}

