using DwebBrowser.MicroService.Core;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.StatusBar;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.NavigationBar;

namespace DwebBrowser.MicroService.Sys.NativeUI;

public class NativeUiNMM : NativeMicroModule
{
    public NativeUiNMM() : base("nativeui.sys.dweb")
    {
    }

    private StatusBarNMM _statusBarNMM = new();
    private NavigationBarNMM _navigationBarNMM = new();

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        bootstrapContext.Dns.Install(_statusBarNMM);
        bootstrapContext.Dns.Install(_navigationBarNMM);
    }
}

