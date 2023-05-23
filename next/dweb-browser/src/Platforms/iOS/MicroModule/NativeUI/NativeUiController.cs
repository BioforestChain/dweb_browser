using UIKit;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.Base;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.SafeArea;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.StatusBar;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.NavigationBar;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.VirtualKeyboard;
using DwebBrowser.MicroService.Sys.Mwebview;
using System.Text.Json;

namespace DwebBrowser.MicroService.Sys.NativeUI;

public class NativeUiController
{
    // TODO: 多个Dwebview共用一个MWebviewController，共享nativeui状态？
    // Android是多个Dwebview公用一个Activity，共享nativeui状态？
    public static NativeUiController FromMultiWebView(Mmid mmid) =>
        ((MultiWebViewNMM.GetCurrentWebViewController(mmid)
            ?? throw new Exception(String.Format("native ui is unavailable for {0}", mmid))).LastViewOrNull
            ?? throw new Exception(String.Format("current webview instance is invalid for {0}", mmid)))
        .nativeUiController;

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

