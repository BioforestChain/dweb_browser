using UIKit;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.Base;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.StatusBar;

namespace DwebBrowser.MicroService.Sys.NativeUI;

public class NativeUiController
{
    public static NativeUiController FromMicroModule(Mmid mmid)
    {
        throw new NotImplementedException();
    }

    public readonly StatusBarController StatusBarController = new();

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
}

