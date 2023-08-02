using DwebBrowser.MicroService.Browser.NativeUI.NavigationBar;
using DwebBrowser.MicroService.Browser.NativeUI.SafeArea;
using DwebBrowser.MicroService.Browser.NativeUI.StatusBar;
using DwebBrowser.MicroService.Browser.NativeUI.VirtualKeyboard;

namespace DwebBrowser.MicroService.Browser.NativeUI;

public class NativeUiNMM : NativeMicroModule
{
    public NativeUiNMM() : base("nativeui.browser.dweb", "nativeui")
    {
    }

    private readonly StatusBarNMM _statusBarNMM = new();
    private readonly NavigationBarNMM _navigationBarNMM = new();
    private readonly VirtualKeyboardNMM _virtualKeyboardNMM = new();
    private readonly SafeAreaNMM _safeAreaNMM = new();

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        bootstrapContext.Dns.Install(_statusBarNMM);
        bootstrapContext.Dns.Install(_navigationBarNMM);
        bootstrapContext.Dns.Install(_virtualKeyboardNMM);
        bootstrapContext.Dns.Install(_safeAreaNMM);
    }
}

