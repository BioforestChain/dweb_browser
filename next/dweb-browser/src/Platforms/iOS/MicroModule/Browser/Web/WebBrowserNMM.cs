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
            await MainThread.InvokeOnMainThreadAsync(async () =>
            {
                BridgeManager.WebviewGeneratorCallbackWithCallback(configuration =>
                {
                    return new BrowserWeb(this, configuration);
                });

                var manager = new BridgeManager();
                var browserView = manager.BrowserView;
                browserView.Frame = WebBrowserController.View.Frame;

                var deskController = await GetDeskController();
                deskController?.AddSubView(browserView);
            });
        };
    }

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

