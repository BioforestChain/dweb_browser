
using UIKit;

namespace DwebBrowser.MicroService.Core;

public static class IOSWindow
{
    public static UIWindow GetWindow()
    {
        UIWindow window;
        if (UIDevice.CurrentDevice.CheckSystemVersion(13, 0))
        {
            // iOS 13 及更高版本
            UIWindowScene windowScene = UIApplication.SharedApplication.KeyWindow.WindowScene;
            window = windowScene.Windows[0];
        }
        else
        {
            // iOS 12 及更低版本
            window = UIApplication.SharedApplication.KeyWindow;
        }

        return window;
    }
}

