using System.Net;
using System.Net.Http.Headers;
using DwebBrowser.DWebView;
using DwebBrowser.MicroService;
using DwebBrowser.MicroService.Core;
using DwebBrowser.MicroService.Message;

namespace demo;

[Register("AppDelegate")]
public class AppDelegate : UIApplicationDelegate
{
    public override UIWindow? Window
    {
        get;
        set;
    }

    class TestNMM : NativeMicroModule
    {
        public TestNMM() : base("test.sys.dweb")
        {
        }

        protected override Task _bootstrapAsync(IBootstrapContext bootstrapContext)
        {
            throw new NotImplementedException();
        }

        protected override Task _onActivityAsync(IpcEvent Event, Ipc ipc)
        {
            throw new NotImplementedException();
        }

        protected override Task _shutdownAsync()
        {
            throw new NotImplementedException();
        }
    }

    public override bool FinishedLaunching(UIApplication application, NSDictionary launchOptions)
    {
        // create a new window instance based on the screen size
        Window = new UIWindow(UIScreen.MainScreen.Bounds);

        // create a UIViewController with a single UILabel
        var vc = new UIViewController();
        var localeNmm = new TestNMM();
        NativeFetch.NativeFetchAdaptersManager.Append(async (mm, request) =>
        {
            if (request.RequestUri?.Host is "test.sys.dweb")
            {
                if (request.RequestUri.AbsolutePath is "/index.html")
                {
                    var response = new HttpResponseMessage(HttpStatusCode.OK);
                    response.Content = new StringContent(@"
                    <h1>你好!!</h1>
                    <h1>你好!!</h1>
                    <h1>你好!!</h1>
                    <h1>你好!!</h1>
                    <h1>你好!!</h1>
                    <h1>你好!!</h1>
                    <script>
                    var a = 1;
                    addEventListener('message',(event)=>{
                        const ele = document.createElement(""h1"");
                        ele.style.color = 'red';
                        ele.innerHTML = [event.data,...event.ports].join("" "");
                        document.body.insertBefore(ele, document.body.firstChild);
                    });
                    </script>
                    ", new MediaTypeHeaderValue("text/html", "utf-8"));
                    return response;
                }
            }
            return null;
        });
        var dwebview = new DWebView(vc.View?.Frame, localeNmm, localeNmm, new DWebView.Options("https://test.sys.dweb/index.html"), null);
        vc.View!.AddSubview(dwebview);
        ////webviewHelper.webview.LoadRequest(new NSUrlRequest(new NSUrl("https://news.cnblogs.com")));
        //WebKit.WKNavigation wKNavigation = dwebview.LoadSimulatedRequest(new NSUrlRequest(new NSUrl("https://baidu.com")), @"
        //<h1>你好</h1>
        //<script>
        //var a = 1;
        //addEventListener('message',(event)=>{
        //    const ele = document.createElement(""h1"");
        //    ele.style.color = 'red';
        //    ele.innerHTML = [event.data,...event.ports].join("" "");
        //    document.body.insertBefore(ele, document.body.firstChild);
        //});
        //</script>
        //");

        var btn = new UIButton();
        btn.SetTitle("创建Channel", UIControlState.Normal);
        btn.SetTitleColor(UIColor.Blue, UIControlState.Normal);
        btn.Frame = new CGRect(100, 100, 100, 30);
        btn.AddTarget(new EventHandler(async (sender, e) =>
        {
            var channel = await dwebview.CreateWebMessageChannelC();

            channel.Port2.OnMessage += async (messageEvent, _) =>
            {
                Console.WriteLine("port2 on message: {0}", messageEvent.Data.ToString());
            };
            _ = Task.Run(async () =>
            {
                var i = 0;
                while (i++ < 5)
                {
                    Console.WriteLine("postMessage {0}", i);
                    await channel.Port1.PostMessage(new WebMessage(new NSString("你好" + i)));
                    await Task.Delay(100);
                    if (i >= 3)
                    {
                        await channel.Port2.Start();
                    }
                }
                await dwebview.PostMessage("你好", new WebMessagePort[] { channel.Port1 });
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

