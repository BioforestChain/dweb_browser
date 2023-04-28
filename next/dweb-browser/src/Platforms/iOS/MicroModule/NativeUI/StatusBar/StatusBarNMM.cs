using System;
using DwebBrowser.MicroService.Sys.NativeUI;
using UIKit;

namespace DwebBrowser.Platforms.iOS.MicroModule.NativeUI.StatusBar;

public class StatusBarNMM : NativeMicroModule
{
    public StatusBarNMM() : base("status-bar.nativeui.sys.dweb")
    {
    }


    private StatusBarController GetController(Mmid mmid) =>
        NativeUiController.FromMicroModule(mmid).StatusBar;

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        HttpRouter.AddRoute(IpcMethod.Get, "/getState", async (_, ipc) =>
        {
            return GetController(ipc.Remote.Mmid);
        });
    }
}

