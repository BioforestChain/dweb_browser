using System;
using DwebBrowser.MicroService;
using DwebBrowser.MicroService.Core;
using DwebBrowser.MicroService.Message;

namespace DwebBrowser.Platforms.iOS.WebModule.Mwebview;

public class MultiWebViewNMM : NativeMicroModule
{
    public MultiWebViewNMM() : base("mwebview.sys.dweb")
    {
    }

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        /// nativeui 与 mwebview 是伴生关系
        bootstrapContext.Dns.BootstrapAsync("nativeui.sys.dweb");


        
    }

    protected override async Task _onActivityAsync(IpcEvent Event, Ipc ipc)
    {
    }

    protected override async Task _shutdownAsync()
    {
    }
}

