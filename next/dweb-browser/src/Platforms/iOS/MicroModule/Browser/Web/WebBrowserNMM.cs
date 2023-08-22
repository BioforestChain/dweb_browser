using DwebBrowser.MicroService.Http;
using DwebBrowserFramework;

namespace DwebBrowser.MicroService.Browser.Web;

public class WebBrowserNMM : IOSNativeMicroModule
{
    static readonly Debugger Console = new("WebNMM");
    public override string ShortName { get; set; } = "Browser";
    public WebBrowserNMM() : base("web.browser.dweb", "Web Browser")
    {
        s_controllerList.Add(new(this));
    }

    private static readonly List<WebBrowserController> s_controllerList = new();
    public static WebBrowserController WebBrowserController
    {
        get => s_controllerList.FirstOrDefault();
    }

    public override List<Dweb_DeepLink> Dweb_deeplinks { get; init; } = new() { "dweb:search" };
    public override List<MicroModuleCategory> Categories { get; init; } = new()
    {
        MicroModuleCategory.Application,
        MicroModuleCategory.Web_Browser,
    };

    public override List<Core.ImageSource> Icons { get { return new() { new Core.ImageSource("file:///sys/browser/web/logo.svg") }; } }

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        var browserServer = CreateBrowserWebServer();

        OnActivity += async (Event, ipc, _) =>
        {
            await OpenBrowserWindow(ipc);
        };

        HttpRouter.AddRoute(IpcMethod.Get, "/search", async (request, ipc) =>
        {
            Console.Log("DoSearch", request.Url);
            var search = request.QueryStringRequired("q");
            await OpenBrowserWindow(ipc, search: search);
            return unit;
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/openinbrowser", async (request, ipc) =>
        {
            Console.Log("openinbrowser", request.ParsedUrl.Uri.ToString());
            var url = request.QueryStringRequired("url");
            await OpenBrowserWindow(ipc, url: url);
            return unit;
        });
    }

    private Task OpenBrowserWindow(Ipc ipc, string? search = null, string? url = null) => MainThread.InvokeOnMainThreadAsync(async () =>
    {
        BridgeManager.WebviewGeneratorCallbackWithCallback(configuration =>
        {
            return new BrowserWeb(this, configuration);
        });

        var manager = new BridgeManager();

        /// 打开安装窗口
        var win = await WindowAdapterManager.Instance.CreateWindow(
            new WindowState(ipc.Remote.Mmid, Mmid, microModule: this) { Mode = WindowMode.MAXIMIZE });
        WindowAdapterManager.Instance.RenderProviders.TryAdd(win.Id, (windowRenderScope, deskAppUIView) =>
        {
            deskAppUIView.Render(manager.BrowserView, windowRenderScope);
        });

        win.OnClose.OnListener += async (_, _) =>
        {
            WindowAdapterManager.Instance.RenderProviders.Remove(win.Id, out _);
        };
    });

    private async Task<HttpDwebServer> CreateBrowserWebServer()
    {
        var server = await CreateHttpDwebServer(new DwebHttpServerOptions(433, ""));

        {
            var url = string.Empty;
            var serverIpc = await server.Listen();
            var API_PREFIX = "/api/";
            serverIpc.OnRequest += async (request, ipc, _) =>
            {
                var pathname = request.Uri.AbsolutePath;
                if (pathname.StartsWith(API_PREFIX))
                {
                    url = string.Format("file://{0}{1}", pathname[API_PREFIX.Length..], request.Uri.Query);
                }
                else
                {
                    url = string.Format($"file:///sys/browser/desk{pathname}?mode=stream");
                }

                var response = await NativeFetchAsync(new PureRequest(url, request.Method, request.Headers, request.Body.ToPureBody()));
                var ipcReponse = response.ToIpcResponse(request.ReqId, ipc);

                await ipc.PostMessageAsync(ipcReponse);
            };
        }

        return server;
    }
}

