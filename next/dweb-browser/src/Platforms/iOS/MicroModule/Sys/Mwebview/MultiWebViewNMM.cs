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

            var viewItem = await _openDwebViewAsync(remoteMM, url);
            return new HttpResponseMessage(HttpStatusCode.OK).Also(it => it.Content = new StringContent(viewItem.webviewId));
        });

        // 关闭指定 webview 窗口
        HttpRouter.AddRoute(IpcMethod.Get, "/close", async (request, ipc) =>
        {
            var webviewId = request.QueryValidate<string>("webview_id")!;
            var remoteMmid = ipc!.Remote.Mmid;

            return await _closeDwebViewAsync(remoteMmid, webviewId);
        });

        // 界面没有关闭，用于重新唤醒
        HttpRouter.AddRoute(IpcMethod.Get, "/activate", async (request, ipc) =>
        {
            var remoteMmid = ipc.Remote.Mmid;
            var webViewId = request.QueryValidate<string>("webview_id")!;

            Console.Log("REOPEN-WEBVIEW", "remote-mmid: {0} ==> {1}", remoteMmid, webViewId);
            OpenActivity(remoteMmid);

            return new HttpResponseMessage(HttpStatusCode.OK).Also(it => it.Content = new StringContent(webViewId));
        });
    }

    public override void OpenActivity(string remoteMmid)
    {
        var vc = IOSWindow.RootViewController;
        var controller = s_controllerMap.GetValueOrDefault(remoteMmid);
        vc?.NavigationController?.PushViewController(controller!, true);
    }

    private async Task<MultiWebViewController.ViewItem> _openDwebViewAsync(MicroModule remoteMM, string url)
    {
        var remoteMmid = remoteMM.Mmid;
        Console.Log("OPEN-WEBVIEW", "remote-mmid: {0} / url: {1}", remoteMmid, url);
        var controller = s_controllerMap.GetValueOrPut(remoteMmid, () => new MultiWebViewController(remoteMmid, this, remoteMM));

        OpenActivity(remoteMmid);
        await _OnActivityEmit(remoteMmid, controller);
        return await controller.OpenWebViewAsync(url);
    }

    private async Task<bool> _closeDwebViewAsync(string remoteMmid, string webviewId)
    {
        var controller = s_controllerMap.GetValueOrDefault(remoteMmid);

        if (controller is not null)
        {
            await controller.CloseWebViewAsync(webviewId);
            await controller.DismissViewControllerAsync(true);
        }

        return false;
    } 
}

