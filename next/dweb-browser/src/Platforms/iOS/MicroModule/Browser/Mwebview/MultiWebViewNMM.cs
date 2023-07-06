using System.Net;

#nullable enable

namespace DwebBrowser.MicroService.Browser.Mwebview;

public class MultiWebViewNMM : IOSNativeMicroModule
{
    static readonly Debugger Console = new("MultiWebViewNMM");

    public MultiWebViewNMM() : base("mwebview.browser.dweb")
    {
    }

    private static Dictionary<Mmid, MultiWebViewController> s_controllerMap = new();
    public static MultiWebViewController? GetCurrentWebViewController(Mmid mmid) => s_controllerMap.GetValueOrDefault(mmid);

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        /// nativeui 与 mwebview 是伴生关系
        await bootstrapContext.Dns.BootstrapAsync("nativeui.browser.dweb");

        // 打开一个webview作为窗口
        HttpRouter.AddRoute(IpcMethod.Get, "/open", async (request, ipc) =>
        {
            var url = request.QueryStringRequired("url");
            var remoteMM = ipc?.AsRemoteInstance() ?? throw new Exception("mwebview.browser.dweb/open should be call by locale");

            ipc.OnClose += async (_) =>
            {
                Console.Log("/open", "listen ipc close destroy window");

                var controller = s_controllerMap.GetValueOrDefault(ipc!.Remote.Mmid);

                if (controller is not null)
                {
                    var vc = await RootViewController.WaitPromiseAsync();

                    await MainThread.InvokeOnMainThreadAsync(async () =>
                    {
                        await controller.DismissViewControllerAsync(true);
                        vc.PopViewController(true);
                    });

                    controller.DestroyWebView();
                }
            };


            var viewItem = await _openDwebViewAsync(remoteMM, url);
            return new HttpResponseMessage(HttpStatusCode.OK).Also(it => it.Content = new StringContent(viewItem.webviewId));
        });

        // 关闭指定 webview 窗口
        HttpRouter.AddRoute(IpcMethod.Get, "/close", async (request, ipc) =>
        {
            var searchParams = request.SafeUrl.SearchParams;
            var webviewId = searchParams.ForceGet("webview_id");
            var remoteMmid = ipc!.Remote.Mmid;

            return await _closeDwebViewAsync(remoteMmid, webviewId);
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/close/app", async (request, ipc) =>
        {
            var controller = s_controllerMap.GetValueOrDefault(ipc!.Remote.Mmid);

            if (controller is not null)
            {
                var vc = await RootViewController.WaitPromiseAsync();

                await MainThread.InvokeOnMainThreadAsync(async () =>
                {
                    await controller.DismissViewControllerAsync(true);
                    vc.PopViewController(true);
                });

                return controller.DestroyWebView();
            }

            return false;
        });

        // 界面没有关闭，用于重新唤醒
        HttpRouter.AddRoute(IpcMethod.Get, "/activate", async (request, ipc) =>
        {
            var remoteMmid = ipc!.Remote.Mmid;
            OpenActivity(remoteMmid);

            return new HttpResponseMessage(HttpStatusCode.OK);
        });
    }

    public override async void OpenActivity(Mmid remoteMmid)
    {
        if (s_controllerMap.TryGetValue(remoteMmid, out var controller))
        {
            var vc = await RootViewController.WaitPromiseAsync();

            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                // 无法push同一个UIViewController的实例两次
                var index = vc.ViewControllers?.ToList().FindIndex(uvc => uvc == controller);
                if (index >= 0)
                {
                    // 不是当前controller时，推到最新
                    if (index != vc.ViewControllers!.Length - 1)
                    {
                        vc.PopToViewController(controller, true);
                    }
                }
                else
                {
                    vc.PushViewController(controller, true);
                }
            });
        }
    }

    private async Task<MultiWebViewController.ViewItem> _openDwebViewAsync(MicroModule remoteMM, string url)
    {
        var remoteMmid = remoteMM.Mmid;
        Console.Log("OPEN-WEBVIEW", "remote-mmid: {0} / url: {1}", remoteMmid, url);

        var controller = await MainThread.InvokeOnMainThreadAsync(() => s_controllerMap.GetValueOrPut(remoteMmid, () => new MultiWebViewController(remoteMmid, this, remoteMM)));

        OpenActivity(remoteMmid);
        await _OnActivityEmit(remoteMmid, controller);
        return await controller.OpenWebViewAsync(url);
    }

    private async Task<bool> _closeDwebViewAsync(string remoteMmid, string webviewId)
    {
        var controller = s_controllerMap.GetValueOrDefault(remoteMmid);

        if (controller is not null)
        {
            var vc = await RootViewController.WaitPromiseAsync();
            await MainThread.InvokeOnMainThreadAsync(async () =>
            {
                await controller.CloseWebViewAsync(webviewId);
                await controller.DismissViewControllerAsync(true);
                vc.PopViewController(true);
            });
        }

        return false;
    }
}

