using DwebBrowser.MicroService.Browser.NativeUI.NavigationBar;
using DwebBrowser.MicroService.Browser.NativeUI.SafeArea;
using DwebBrowser.MicroService.Browser.NativeUI.StatusBar;
using DwebBrowser.MicroService.Browser.NativeUI.VirtualKeyboard;

namespace DwebBrowser.MicroService.Browser.NativeUI;

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

