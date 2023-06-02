namespace DwebBrowser.MicroService.Browser.NativeUI.StatusBar;

public class StatusBarNMM : NativeMicroModule
{
    static readonly Debugger Console = new("StatusBarNMM");
    public StatusBarNMM() : base("status-bar.nativeui.browser.dweb")
    {
    }

    private Task<StatusBarController> _getControllerAsync(Mmid mmid) =>
        MainThread.InvokeOnMainThreadAsync(() => NativeUiController.FromMultiWebView(mmid).StatusBar);

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        HttpRouter.AddRoute(IpcMethod.Get, "/getState", async (_, ipc) =>
        {
            return await _getControllerAsync(ipc.Remote.Mmid);
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/setState", async (request, ipc) =>
        {
            var controller = await _getControllerAsync(ipc.Remote.Mmid);
            request.QueryColor("color")?.Also(it =>
            {
                controller.ColorState.Set(it);
                //Console.Log("setState", "red: {0}, green: {1}, blue: {2}, alpha: {3}",
                //    it.red, it.green, it.blue, it.alpha);
                controller.Observer.Get();
            });
            request.QueryStyle("style")?.Also(it =>
            {
                controller.StyleState.Set(it);
                controller.Observer.Get();
            });
            request.QueryBool("overlay")?.Also(it =>
            {
                controller.OverlayState.Set(it);
                controller.Observer.Get();
            });
            request.QueryBool("visible")?.Also(it =>
            {
                controller.VisibleState.Set(it);
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

