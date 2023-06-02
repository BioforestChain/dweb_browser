using CoreGraphics;
using Foundation;
using UIKit;
using WebKit;
using SwiftUIMAUILibrary;
using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Platform;

namespace MAUIProject;

[Register("AppDelegate")]
public class AppDelegate : MauiUIApplicationDelegate
{
	protected override MauiApp CreateMauiApp() => MauiProgram.CreateMauiApp();
    

    public override bool FinishedLaunching(UIApplication application, NSDictionary launchOptions)
    {
        _ = base.FinishedLaunching(application, launchOptions);

        Window = application.KeyWindow;
        var vc = new MyViewController();
        var nav = new UINavigationController(vc);
        Window.RootViewController = nav;
        Window.MakeKeyAndVisible();

        
        return true;
    }

    public class MyViewController : UIViewController
    {
        public override void ViewDidLoad()
        {
            base.ViewDidLoad();

            // 设置视图控制器的背景颜色
            View.BackgroundColor = UIColor.Yellow;

            var frame = new Rect(x: 0, y: 0, width: 100, height: 100);
            var webView = new WKWebView(frame, new WKWebViewConfiguration());
            var webs = new WKWebView[] { webView };

            var control = new BrowserControl();
            var handler = new BrowserHandler();
            control.Value = webs;

            
        }
    }
}

