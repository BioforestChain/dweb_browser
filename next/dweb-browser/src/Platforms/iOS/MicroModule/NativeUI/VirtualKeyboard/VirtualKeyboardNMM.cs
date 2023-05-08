
using DwebBrowser.MicroService.Sys.NativeUI;

namespace DwebBrowser.Platforms.iOS.MicroModule.NativeUI.VirtualKeyboard;

public class VirtualKeyboardNMM : NativeMicroModule
{
    public VirtualKeyboardNMM() : base("virtual-keyboard.nativeui.sys.dweb")
    {
    }

    static Debugger Console = new("VirtualKeyboardNMM");

    private VirtualKeyboardController _getController(Mmid mmid) =>
        NativeUiController.FromMultiWebView(mmid).VirtualKeyboard;

    // TODO：数据订阅路由功能未完成
    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        /// 获取状态
        HttpRouter.AddRoute(IpcMethod.Get, "/getState", async (_, ipc) =>
        {
            var controller = _getController(ipc.Remote.Mmid);
            Console.Log("virtual-keyboard getState", controller.OverlayState.Get().ToString());
            return controller;
        });

        /// 设置状态
        HttpRouter.AddRoute(IpcMethod.Get, "/setState", async (request, ipc) =>
        {
            var controller = _getController(ipc.Remote.Mmid);
            request.QueryValidate<bool>("overlay", false).Also(it =>
                controller.OverlayState.Update(cache => cache = it));
            request.QueryValidate<bool>("visible", false).Also(it =>
                controller.VisibleState.Update(cache => cache = it));

            return null;
        });

        /// 开始数据订阅
        HttpRouter.AddRoute(IpcMethod.Get, "/startObserve", async (_, ipc) =>
        {
            return _getController(ipc.Remote.Mmid).StateObserver.StartObserve(ipc);
        });

        /// 停止数据订阅
        HttpRouter.AddRoute(IpcMethod.Get, "/stopObserve", async (_, ipc) =>
        {
            return _getController(ipc.Remote.Mmid).StateObserver.StopObserve(ipc);
        });
    }
}

