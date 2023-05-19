
using DwebBrowser.MicroService.Sys.NativeUI;

namespace DwebBrowser.Platforms.iOS.MicroModule.NativeUI.VirtualKeyboard;

public class VirtualKeyboardNMM : NativeMicroModule
{
    static Debugger Console = new("VirtualKeyboardNMM");
    public VirtualKeyboardNMM() : base("virtual-keyboard.nativeui.sys.dweb")
    {
    }

    private Task<VirtualKeyboardController> _getControllerAsync(Mmid mmid) =>
        MainThread.InvokeOnMainThreadAsync(() => NativeUiController.FromMultiWebView(mmid).VirtualKeyboard);

    // TODO：数据订阅路由功能未完成
    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        /// 获取状态
        HttpRouter.AddRoute(IpcMethod.Get, "/getState", async (_, ipc) =>
        {
            return await _getControllerAsync(ipc.Remote.Mmid);
        });

        /// 设置状态
        HttpRouter.AddRoute(IpcMethod.Get, "/setState", async (request, ipc) =>
        {
            var controller = await _getControllerAsync(ipc.Remote.Mmid);

            request.QueryBool("overlay")?.Also(it =>
            {
                controller.OverlayState.Set(it);
                controller.Observer.Get();
            });

            /// 通过虚拟键盘事件来变更显示隐藏
            //request.QueryBool("visible")?.Also(it =>
            //    controller.VisibleState.Set(it));

            return null;
        });

        /// 开始数据订阅
        HttpRouter.AddRoute(IpcMethod.Get, "/startObserve", async (_, ipc) =>
        {
            return (await _getControllerAsync(ipc.Remote.Mmid)).StateObserver.StartObserve(ipc);
        });

        /// 停止数据订阅
        HttpRouter.AddRoute(IpcMethod.Get, "/stopObserve", async (_, ipc) =>
        {
            return (await _getControllerAsync(ipc.Remote.Mmid)).StateObserver.StopObserve(ipc);
        });
    }
}

