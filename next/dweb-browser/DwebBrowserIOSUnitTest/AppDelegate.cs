using CoreGraphics;
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

        Debugger.DebugScopes = new() { "*" };
        Debugger.DebugTags = new() { "*" };
        // create a UIViewController with a single UILabel
        var vc = new FirstUIViewController();
        //vc.View.Frame = UIScreen.MainScreen.Bounds;
        Window.RootViewController = vc;

        // make the window visible
        Window.MakeKeyAndVisible();

        return true;
    }

    public class TestUIView : UIView
    {
        static readonly Debugger Console = new("TestUIView");
        public override void WillRemoveSubview(UIView uiview)
        {
            Console.Log("WillRemoveSubview", uiview.Tag.ToString());
            //uiview.RemoveFromSuperview();
        }

        public override void RemoveFromSuperview()
        {
            Console.Log("RemoveFromSuperview", Tag.ToString());
            base.RemoveFromSuperview();
        }
    }

    public class FirstUIViewController : UIViewController
    {
        //public UIView TestView = new();
        public TestUIView TestView = new();

        public override void ViewDidLoad()
        {
            base.ViewDidLoad();

            TestView.Frame = UIScreen.MainScreen.Bounds;
            TestView.BackgroundColor = UIColor.White;
            View?.AddSubview(TestView);

            var btn = new UIButton();
            btn.SetTitle("添加View", UIControlState.Normal);
            btn.SetTitleColor(UIColor.Blue, UIControlState.Normal);
            btn.Frame = new CGRect(200, 100, 100, 30);
            btn.AddTarget(new EventHandler(async (sender, e) =>
            {
                var uiview = new TestUIView()
                {
                    Tag = 101,
                    Frame = new CGRect(100, 300, 100, 30),
                    BackgroundColor = UIColor.Red
                };
                TestView.AddSubview(uiview);
            })
            , UIControlEvent.TouchUpInside);

            var btn1 = new UIButton();
            btn1.SetTitle("移除View", UIControlState.Normal);
            btn1.SetTitleColor(UIColor.Blue, UIControlState.Normal);
            btn1.Frame = new CGRect(100, 200, 100, 30);
            btn1.AddTarget(new EventHandler(async (sender, e) =>
            {
                foreach (var view in TestView.Subviews)
                {
                    //if (view is TestUIView testView && testView.Tag == 101)
                    //{
                    //    testView.RemoveFromSuperview();
                    //}

                    if (view.Tag == 101)
                    {
                        //TestView.WillRemoveSubview(view);
                        view.RemoveFromSuperview();
                    }
                }
            })
            , UIControlEvent.TouchUpInside);

            TestView.AddSubviews(btn, btn1);
        }
    }
}

