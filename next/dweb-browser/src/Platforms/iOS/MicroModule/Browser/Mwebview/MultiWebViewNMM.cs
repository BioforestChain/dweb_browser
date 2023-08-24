using System.Net;
using DwebBrowser.MicroService.Http;

#nullable enable

namespace DwebBrowser.MicroService.Browser.Mwebview;

public class MultiWebViewNMM : IOSNativeMicroModule
{
    static readonly Debugger Console = new("MultiWebViewNMM");

    public override string? ShortName { get; set; } = "MWebview";
    public MultiWebViewNMM() : base("mwebview.browser.dweb", "Multi Webview Renderer")
    {
    }

    public override List<MicroModuleCategory> Categories { get; init; } = new()
    {
        MicroModuleCategory.Service,
        MicroModuleCategory.Render_Service,
    };

    private static readonly Dictionary<Mmid, MultiWebViewController> s_controllerMap = new();
    public static MultiWebViewController? GetCurrentWebViewController(Mmid mmid) => s_controllerMap.GetValueOrDefault(mmid);

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        /// nativeui 与 mwebview 是伴生关系
        await bootstrapContext.Dns.Open("nativeui.browser.dweb");

        /// 打开一个 webview，并将它以 窗口window 的标准进行展示
        HttpRouter.AddRoute(IpcMethod.Get, "/open", async (request, ipc) =>
        {
            var url = request.QueryStringRequired("url");
            var remoteMM = ipc?.AsRemoteInstance() ?? throw new Exception("mwebview.browser.dweb/open should be call by locale");

            ipc.OnClose += async (_) =>
            {
                Console.Log("/open", "listen ipc close destroy window");

                var controller = s_controllerMap.GetValueOrDefault(ipc!.Remote.Mmid);
                controller?.DestroyWebView();
            };

            var (viewItem, controller) = await OpenDwebViewAsync(remoteMM, url, ipc);

            return new ViewItemResponse(viewItem.webviewId, controller.Win.Id);
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
                return controller.DestroyWebView();
            }

            return false;
        });

        // 界面没有关闭，用于重新唤醒
        HttpRouter.AddRoute(IpcMethod.Get, "/activate", async (request, ipc) =>
        {
            var remoteMmid = ipc!.Remote.Mmid;
            var controller = s_controllerMap.GetValueOrDefault(remoteMmid);

            if (controller is not null)
            {
                return false;
            }

            return new PureResponse(HttpStatusCode.OK);
        });
    }

    public record ViewItemResponse(string webviewId, UUID wid);

    private async Task<(MultiWebViewController.ViewItem, MultiWebViewController)> OpenDwebViewAsync(
        MicroModule remoteMM,
        string url,
        Ipc ipc)
    {
        var remoteMmid = remoteMM.Mmid;
        Console.Log("OPEN-WEBVIEW", "remote-mmid: {0} / url: {1}", remoteMmid, url);

        var controller = await MainThread.InvokeOnMainThreadAsync(() => s_controllerMap.GetValueOrPutAsync(remoteMmid, async () =>
        {
            var windowState = new WindowState(ipc.Remote.Mmid, Mmid, microModule: this);
            try
            {
                /// 挑选合适的图标作为应用的图标
                var iconResource = ipc.Remote.Icons?.Let(icons =>
                {
                    var comparableBuilder = ComparableWrapper<StrictImageResource>.Builder(imageResource =>
                    {
                        return new()
                        {
                            {
                                "purpose",
                                EnumComparable.EnumToComparable<ImageResourcePurposes>(imageResource.Purpose,
                                new List<ImageResourcePurposes> { ImageResourcePurposes.Any }).First()
                            },
                            {
                                "type",
                                EnumComparable.EnumToComparable(imageResource.Type,
                                new List<string> { "image/svg+xml", "image/png", "image/jpeg", "image/*" })
                            },
                            {
                                "area",
                                imageResource.Sizes.Last().Let(it => -it.Width * it.Height)
                            }
                        };
                    });

                    var imageResource = icons.Min();

                    if (imageResource is not null)
                    {
                        return comparableBuilder.Build(StrictImageResource.From(imageResource)).Value;
                    }

                    return null;
                });

                if (iconResource is not null)
                {
                    windowState.IconUrl = iconResource.Src;
                    windowState.IconMaskable = iconResource.Purpose.Contains(ImageResourcePurposes.Maskable);
                    windowState.IconMonochrome = iconResource.Purpose.Contains(ImageResourcePurposes.Monochrome);
                }
            }
            catch (Exception e)
            {
                Console.Error("windowstate", e.Message);
            }


            var win = await WindowAdapterManager.Instance.CreateWindow(windowState);
            return new MultiWebViewController(win, ipc, remoteMM, this).Also(controller =>
            {
                /// 窗口销毁的时候，释放这个Controller
                win.OnClose.OnListener += async (_, _) =>
                {
                    s_controllerMap.Remove(remoteMmid);
                };
            });
        }));


        return (await controller.OpenWebViewAsync(url), controller);
    }

    private async Task<bool> _closeDwebViewAsync(string remoteMmid, string webviewId)
    {
        var controller = s_controllerMap.GetValueOrDefault(remoteMmid);

        if (controller is not null)
        {
            //var vc = await RootViewController.WaitPromiseAsync();
            await MainThread.InvokeOnMainThreadAsync(async () =>
            {
                await controller.CloseWebViewAsync(webviewId);
                //await controller.DismissViewControllerAsync(true);
                //vc.PopViewController(true);
            });
        }

        return false;
    }
}

