using DwebBrowser.MicroService.Core;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.SafeArea;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.StatusBar;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.NavigationBar;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.VirtualKeyboard;

namespace DwebBrowser.MicroService.Sys.NativeUI;

public class NativeUiNMM : NativeMicroModule
{
    public NativeUiNMM() : base("nativeui.browser.dweb")
    {
    }

    private StatusBarNMM _statusBarNMM = new();
    private NavigationBarNMM _navigationBarNMM = new();
    private VirtualKeyboardNMM _virtualKeyboardNMM = new();
    private SafeAreaNMM _safeAreaNMM = new();

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        bootstrapContext.Dns.Install(_statusBarNMM);
        bootstrapContext.Dns.Install(_navigationBarNMM);
        bootstrapContext.Dns.Install(_virtualKeyboardNMM);
        bootstrapContext.Dns.Install(_safeAreaNMM);
    }
}

