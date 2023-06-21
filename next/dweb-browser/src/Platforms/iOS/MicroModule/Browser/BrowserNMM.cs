using System.Net;
using BrowserFramework;
using DwebBrowser.MicroService.Browser.Jmm;
using DwebBrowser.MicroService.Http;
using Foundation;
using UIKit;
using System.Text.Json.Serialization;

namespace DwebBrowser.MicroService.Browser;

public class BrowserNMM : IOSNativeMicroModule
{
    static Debugger Console = new("BrowserNMM");
    public BrowserNMM() : base("browser.dweb")
    {
        s_controllerList.Add(new(this));
    }

    private static readonly List<BrowserController> s_controllerList = new();
    public static BrowserController BrowserController
    {
        get => s_controllerList.FirstOrDefault();
    }

    //record AppInfo(string id, string icon, string name, string short_name);
    class AppInfo
    {
        [JsonPropertyName("id")]
        public string Id { get; set; }
        [JsonPropertyName("icon")]
        public string Icon { get; set; }
        [JsonPropertyName("name")]
        public string Name { get; set; }
        [JsonPropertyName("short_name")]
        public string ShortName { get; set; }

        [Obsolete("使用带参数的构造函数", true)]
#pragma warning disable CS8618 // 在退出构造函数时，不可为 null 的字段必须包含非 null 值。请考虑声明为可以为 null。
        public AppInfo()
#pragma warning restore CS8618 // 在退出构造函数时，不可为 null 的字段必须包含非 null 值。请考虑声明为可以为 null。
        {
            /// 给JSON反序列化用的空参数构造函数
        }

        public AppInfo(string id, string icon, string name, string short_name)
        {
            Id = id;
            Icon = icon;
            Name = name;
            ShortName = short_name;
        }
    }

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        await bootstrapContext.Dns.BootstrapAsync("jmm.browser.dweb");

        HttpRouter.AddRoute(IpcMethod.Get, "/openApp", async (request, ipc) =>
        {
            var mmid = request.QueryStringRequired("app_id");
            return BrowserController?.OpenApp(mmid);
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/appsInfo", async (request, ipc) =>
        {
            var apps = JmmNMM.JmmApps;
            Console.Log("appInfo", "size: {0}", apps.Count);
            var responseApps = new List<AppInfo> { };

            foreach (var app in apps)
            {
                var metadata = app.Value.Metadata;
                responseApps.Add(new AppInfo(metadata.Id, metadata.Icon, metadata.Name, metadata.ShortName));
            }

            return responseApps;
        });

        // 关闭App后端
        HttpRouter.AddRoute(IpcMethod.Get, "/closeApp", async (request, ipc) =>
        {
            var mmid = request.QueryStringRequired("app_id");
            return BrowserController?.CloseApp(mmid);
        });

        // App详情
        HttpRouter.AddRoute(IpcMethod.Get, "/detailApp", async (request, ipc) =>
        {
            var mmid = request.QueryStringRequired("app_id");
            var jmmApps = JmmNMM.JmmApps;
            var jsMicroModule = jmmApps.GetValueOrDefault(mmid);

            if (jsMicroModule is not null)
            {
                var data = NSData.FromString(jsMicroModule.Metadata.ToJson(), NSStringEncoding.UTF8);
                var initDownloadStatus = DownloadStatus.Installed;

                var vc = await RootViewController.WaitPromiseAsync();
                await MainThread.InvokeOnMainThreadAsync(async () =>
                {
                    var manager = new DownloadAppManager(data, (nint)initDownloadStatus);

                    manager.DownloadView.Frame = UIScreen.MainScreen.Bounds;
                    JmmNMM.JmmController.View.AddSubview(manager.DownloadView);
                    vc.PushViewController(JmmNMM.JmmController, true);
                });

                return true;
            }

            return new PureResponse(HttpStatusCode.NotFound, Body: new PureUtf8StringBody("not found " + mmid));
        });
    }

    public override async void OpenActivity(Mmid remoteMmid)
    {
        await MainThread.InvokeOnMainThreadAsync(async () =>
        {
            BrowserManager.WebviewGeneratorCallbackWithCallback(configuration =>
            {
                return new BrowserWeb(this, configuration);
            });
            var manager = new BrowserManager();
            //var webview = new BrowserWeb();
            //webview.LoadRequest(new NSUrlRequest(new NSUrl("https://dweb.waterbang.top/")));
            //manager.WebViewList = new WKWebView[] { webview };
            //manager.ShowWebViewListDataWithList(new WKWebView[] { webview });
            //manager.OpenWebViewUrlWithUrlString("https://dweb.waterbang.top/");
            manager.OpenWebViewUrlWithUrlString("about://newtab");
            var swiftView = manager.SwiftView;
            swiftView.Frame = UIScreen.MainScreen.Bounds;
            BrowserController.View.AddSubview(swiftView);
            //webview.LoadRequest(new NSUrlRequest(new NSUrl("dweb:install?url=https://dweb.waterbang.top/metadata.json")));
        });
    }

    protected override async Task _onActivityAsync(IpcEvent Event, Ipc ipc)
    {
        OpenActivity(ipc.Remote.Mmid);
    }
}

