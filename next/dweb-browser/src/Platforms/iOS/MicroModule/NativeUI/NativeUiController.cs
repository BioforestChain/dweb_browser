using UIKit;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.Base;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.StatusBar;
using DwebBrowser.MicroService.Sys.Mwebview;
using System.Text.Json;

namespace DwebBrowser.MicroService.Sys.NativeUI;

public class NativeUiController
{
    // TODO: 多个Dwebview共用一个MWebviewController，共享nativeui状态？
    // Android是多个Dwebview公用一个Activity，共享nativeui状态？
    public static NativeUiController FromMicroModule(Mmid mmid) =>
        new(MultiWebViewNMM.GetCurrentWebViewController(mmid)
            ?? throw new Exception(String.Format("current webview instance is invalid for {0}", mmid)));

    public readonly StatusBarController StatusBar = new();

    public NativeUiController(UIViewController activity)
    {
        //var a = UIApplication.SharedApplication.StatusBarFrame;
        //      //UIApplication.SharedApplication.Keybo

        //      var x = UIApplication.SharedApplication.StatusBarStyle;
        //      var xx = UIApplication.SharedApplication.StatusBarTintColor;
        //      UIApplication.SharedApplication.SetStatusBarHidden()

        //      NSNotificationCenter.DefaultCenter.AddObserver(UIKeyboard.WillShowNotification, OnKeyboardShow);
        //      NSNotificationCenter.DefaultCenter.AddObserver(UIKeyboard.WillHideNotification, OnKeyboardHide);
    }

    static NativeUiController()
    {
        NativeMicroModule.ResponseRegistry.RegistryJsonAble<ColorJson>(typeof(ColorJson), item =>
            JsonSerializer.Serialize(item, typeof(ColorJson)));
    }
}

