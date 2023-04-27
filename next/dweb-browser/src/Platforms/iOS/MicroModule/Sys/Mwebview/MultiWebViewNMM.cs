using DwebBrowser.MicroService;
using DwebBrowser.MicroService.Core;
using DwebBrowser.MicroService.Message;
using System.Net;
using UIKit;

#nullable enable

namespace DwebBrowser.MicroService.Sys.Mwebview;

public class MultiWebViewNMM : IOSNativeMicroModule
{
    static Debugger Console = new Debugger("MultiWebViewNMM");

    public MultiWebViewNMM() : base("mwebview.sys.dweb")
    {
    }

    private static Dictionary<Mmid, MultiWebViewController> s_controllerMap = new();
    private static MultiWebViewController? s_getCurrentWebViewController(Mmid mmid) => s_controllerMap.GetValueOrDefault(mmid);

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        /// nativeui 与 mwebview 是伴生关系
        await bootstrapContext.Dns.BootstrapAsync("nativeui.sys.dweb");

        // 打开一个webview作为窗口
        HttpRouter.AddRoute(IpcMethod.Get, "/open", async (request, ipc) =>
        {
            var url = request.QueryValidate<string>("url")!;
            var remoteMM = ipc.AsRemoteInstance() ?? throw new Exception("mwebview.sys.dweb/open should be call by locale");


            return new HttpResponseMessage(HttpStatusCode.OK);
        });

        //HttpRouter.AddRoute(IpcMethod.Get, "/close", async (request, ipc) =>
        //{

        //});
    }

    public override void OpenActivity(string remoteMmid)
    {
        //var window = IOSWindow.GetWindow();
        //var vc = window.RootViewController;
        
        //(vc as UINavigationController).PushViewController();
    }

    private Task<MultiWebViewController.ViewItem> _openDwebView(MicroModule remoteMM, string url)
    {
        var remoteMmid = remoteMM.Mmid;
        Console.Log("OPEN-WEBVIEW", String.Format("remote-mmid: {0} / url: {1}", remoteMmid, url));
        var controller = s_controllerMap.GetValueOrPut(remoteMmid, () => new MultiWebViewController(remoteMmid, this, remoteMM));

        OpenActivity(remoteMmid);
        //_OnActivityEmit(remoteMmid, controller.);
        //controller.ViewItem;

    }
}

