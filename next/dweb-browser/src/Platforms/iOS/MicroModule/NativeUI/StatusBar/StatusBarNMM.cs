using System;
using DwebBrowser.MicroService.Sys.NativeUI;
using UIKit;

namespace DwebBrowser.Platforms.iOS.MicroModule.NativeUI.StatusBar;

public class StatusBarNMM : NativeMicroModule
{
    public StatusBarNMM() : base("status-bar.nativeui.sys.dweb")
    {
    }


    private StatusBarController _getController(Mmid mmid) =>
        NativeUiController.FromMultiWebView(mmid).StatusBar;

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        HttpRouter.AddRoute(IpcMethod.Get, "/getState", async (_, ipc) =>
        {
            return _getController(ipc.Remote.Mmid);
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/setState", async (request, ipc) =>
        {
            var controller = _getController(ipc.Remote.Mmid);
            QueryHelper.QueryColor(request)?.Also(it =>
                controller.ColorState.Update(cache => cache = it));
            QueryHelper.QueryStyle(request)?.Also(it =>
                controller.StyleState.Update(cache => cache = it));
            QueryHelper.QueryOverlay(request)?.Also(it =>
                controller.OverlayState.Update(cache => cache = it));
            QueryHelper.QueryVisible(request)?.Also(it =>
                controller.VisibleState.Update(cache => cache = it));
            return null;
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/startObserve", async (_, ipc) =>
        {
            return _getController(ipc.Remote.Mmid).StateObserver.StartObserve(ipc);
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/stopObserve", async (_, ipc) =>
        {
            return _getController(ipc.Remote.Mmid).StateObserver.StopObserve(ipc);
        });
    }
}

