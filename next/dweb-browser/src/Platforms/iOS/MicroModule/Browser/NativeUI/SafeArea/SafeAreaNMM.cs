namespace DwebBrowser.MicroService.Browser.NativeUI.SafeArea;

public class SafeAreaNMM : NativeMicroModule
{
    public SafeAreaNMM() : base("safe-area.nativeui.browser.dweb")
    {
    }

    private Task<SafeAreaController> _getControllerAsync(Mmid mmid) =>
        MainThread.InvokeOnMainThreadAsync(() => NativeUiController.FromMultiWebView(mmid).SafeArea);

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        HttpRouter.AddRoute(IpcMethod.Get, "/getState", async (_, ipc) =>
        {
            return await _getControllerAsync(ipc.Remote.Mmid);
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/setState", async (request, ipc) =>
        {
            var controller = await _getControllerAsync(ipc.Remote.Mmid);
            request.QueryBool("overlay")?.Also(it =>
            {
                controller.OverlayState.Set(it);
                controller.Observer.Get();
            });
            return null;
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/startObserve", async (_, ipc) =>
        {
            return (await _getControllerAsync(ipc.Remote.Mmid)).StateObserver.StartObserve(ipc);
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/stopObserve", async (_, ipc) =>
        {
            return (await _getControllerAsync(ipc.Remote.Mmid)).StateObserver.StopObserve(ipc);
        });
    }
}

