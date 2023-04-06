using DwebBrowser.DWebView;
namespace demo;

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

        // create a UIViewController with a single UILabel
        var vc = new UIViewController();
        var dwebview = DWebView.Create(vc.View?.Frame);
        vc.View!.AddSubview(dwebview);
        //webviewHelper.webview.LoadRequest(new NSUrlRequest(new NSUrl("https://news.cnblogs.com")));
        WebKit.WKNavigation wKNavigation = dwebview.LoadSimulatedRequest(new NSUrlRequest(new NSUrl("https://baidu.com")), @"
        <h1>你好</h1>
        <script>
        var a = 1;
        addEventListener('message',(event)=>{
            const ele = document.createElement(""h1"");
            ele.style.color = 'red';
            ele.innerHTML = [event.data,...event.ports].join("" "");
            document.body.insertBefore(ele, document.body.firstChild);
        });
        </script>
        ");

        var btn = new UIButton();
        btn.SetTitle("创建Channel", UIControlState.Normal);
        btn.SetTitleColor(UIColor.Blue, UIControlState.Normal);
        btn.Frame = new CGRect(100, 100, 100, 30);
        btn.AddTarget(new EventHandler(async (sender, e) =>
        {
            var channel = await dwebview.createWebMessageChannel();
            channel.port1.OnMessage += async (messageEvent, _) =>
            {
                Console.WriteLine("port1 on message: {0}", messageEvent.Data.ToString());
            };
            await channel.port1.Start();
            channel.port2.OnMessage += async (messageEvent, _) =>
            {
                Console.WriteLine("port2 on message: {0}", messageEvent.Data.ToString());
            };
            await channel.port2.Start();
            _ = Task.Run(async () =>
            {
                var i = 0;
                while (i++ < 3)
                {
                    Console.WriteLine("postMessage {0}", i);
                    await channel.port1.PostMessage(new WebMessage(new NSString("你好" + i)));
                    await Task.Delay(500);
                }

                await dwebview.PostMessage("你好", new WebMessagePort[] { channel.port1 });
            });
        })
        , UIControlEvent.TouchUpInside);

        vc.View.AddSubview(btn);

        Window.RootViewController = vc;

        // make the window visible
        Window.MakeKeyAndVisible();

        return true;
    }
}

