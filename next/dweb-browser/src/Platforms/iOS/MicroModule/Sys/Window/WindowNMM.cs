using DwebBrowser.MicroService.Browser.Desk;
using System.Net;
using System.Text.Json;
using DwebBrowser.MicroService.Http;

namespace DwebBrowser.MicroService.Sys.Window;

/// <summary>
/// 标准化窗口管理模块
///
/// 该模块暂时不会下放到 std 级别，std 级别通常属于非常底层的中立标准，比如通讯等与客观物理相关的，std是一个dweb平台的最小子集，未来可以基于该标准做认证平台。
/// 而sys级别拥有各异的实现，不同的厂商可以在这个级别做自己的操作系统标准化设计。
/// 这里的windows.sys.dweb属于当下这个时代的一种矩形窗口化设计，它不代表所有的窗口形态，它有自己的取舍。
/// </summary>
public class WindowNMM : NativeMicroModule
{
	public WindowNMM() : base("window.sys.dweb", "Window Management")
	{
	}

    static WindowNMM()
    {
        ResponseRegistry.RegistryJsonAble<WindowState>(
            typeof(WindowState), it => it.ToJsonAble());
        ResponseRegistry.RegistryJsonAble<WindowController>(
            typeof(WindowController), it => it.ToJsonAble());
    }

    record WindowBarStyle(string? contentColor, string? backgroundColor, bool? overlay);

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        WindowController getWindow(PureRequest request)
        {
            var wid = request.QueryStringRequired("wid");
            return WindowInstancesManager.WindowInstancesManagerInstance.Instances.Get(wid) ??
                throw new Exception($"No Found by window id: '{wid}'");
        }

        /// 窗口的状态监听
        HttpRouter.AddRoute(IpcMethod.Get, "/observe", async (request, ipc) =>
        {
            var win = getWindow(request);
            var stream = new ReadableStream(onStart: controller =>
            {
                Signal<Observable.Change> off = async (_, _) =>
                {
                    await controller.EnqueueAsync((JsonSerializer.Serialize(win.State.ToJsonAble()) + "\n").ToUtf8ByteArray());
                };

                win.State.Observable.Listener.OnListener += off;

                _ = off.Emit(new Observable.Change(WindowPropertyKeys.Any.FieldName, null, null)).NoThrow();

                ipc.OnClose += async (_) =>
                {
                    win.State.Observable.Listener.OnListener -= off;
                };
            });

            return new PureResponse(HttpStatusCode.OK, Body: new PureStreamBody(stream.Stream));
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/getState", async (request, _) =>
        {
            return getWindow(request).ToJsonAble();
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/focus", async (request, _) =>
        {
            await getWindow(request).Focus();
            return unit;
        });
        HttpRouter.AddRoute(IpcMethod.Get, "/blur", async (request, _) =>
        {
            await getWindow(request).Blur();
            return unit;
        });
        HttpRouter.AddRoute(IpcMethod.Get, "/maximize", async (request, _) =>
        {
            await getWindow(request).Maximize();
            return unit;
        });
        HttpRouter.AddRoute(IpcMethod.Get, "/unMaximize", async (request, _) =>
        {
            await getWindow(request).UnMaximize();
            return unit;
        });
        HttpRouter.AddRoute(IpcMethod.Get, "/minimize", async (request, _) =>
        {
            await getWindow(request).Minimize();
            return unit;
        });
        HttpRouter.AddRoute(IpcMethod.Get, "/close", async (request, _) =>
        {
            await getWindow(request).Close();
            return unit;
        });
        HttpRouter.AddRoute(IpcMethod.Get, "/setTopBarStyle", async (request, _) =>
        {
            var contentColor = request.QueryString("contentColor");
            var backgroundColor = request.QueryString("backgroundColor");
            var overlay = request.QueryBool("overlay");
            
            await getWindow(request).SetTopBarStyle(contentColor, backgroundColor, overlay);
            return unit;
        });
        HttpRouter.AddRoute(IpcMethod.Get, "/setBottomBarStyle", async (request, _) =>
        {
            var contentColor = request.QueryString("contentColor");
            var backgroundColor = request.QueryString("backgroundColor");
            var overlay = request.QueryBool("overlay");
            var theme = request.QueryString("theme");

            await getWindow(request).SetBottomBarStyle(contentColor, backgroundColor, overlay, theme);
            return unit;
        });
    }
}

