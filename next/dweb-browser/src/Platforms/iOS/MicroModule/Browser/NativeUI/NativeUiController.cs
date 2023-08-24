using DwebBrowser.MicroService.Browser.Mwebview;
using DwebBrowser.MicroService.Browser.NativeUI.NavigationBar;
using DwebBrowser.MicroService.Browser.NativeUI.SafeArea;
using DwebBrowser.MicroService.Browser.NativeUI.StatusBar;
using DwebBrowser.MicroService.Browser.NativeUI.VirtualKeyboard;

namespace DwebBrowser.MicroService.Browser.NativeUI;

public class NativeUiController
{
    // TODO: 多个Dwebview共用一个MWebviewController，共享nativeui状态？
    // Android是多个Dwebview公用一个Activity，共享nativeui状态？
    public static NativeUiController FromMultiWebView(Mmid mmid) =>
        throw new Exception(string.Format("native ui is unavailable for {0}", mmid));

    public StatusBarController StatusBar { get; init; }
    public NavigationBarController NavigationBar { get; init; }
    public VirtualKeyboardController VirtualKeyboard { get; init; }
    public SafeAreaController SafeArea { get; init; }

    static NativeUiController()
    {
        _ = new QueryHelper();
    }

    public NativeUiController(MultiWebViewController mwebviewController)
    {
        StatusBar = new(mwebviewController, this);
        NavigationBar = new(mwebviewController, this);
        VirtualKeyboard = new(mwebviewController, this);
        SafeArea = new(mwebviewController, this);
    }
}

