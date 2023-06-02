using UIKit;
using Foundation;
using CoreGraphics;

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

            // 设置视图控制器的背景颜色
            View.BackgroundColor = UIColor.Yellow;

            // 创建一个UILabel实例
            var label = new UILabel
            {
                Text = "Hello, worldxxxx!",
                TextColor = UIColor.Black,
                TextAlignment = UITextAlignment.Center
            };
            label.Frame = new CGRect(100, 100, 200, 30);

            // 将UILabel添加到视图控制器的视图中
            View.AddSubview(label);

        }
    }

}

