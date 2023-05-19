using UIKit;
using CoreGraphics;
using DwebBrowser.Base;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.Base;

namespace DwebBrowser.MicroService.Sys.Mwebview;

public partial class MultiWebViewController : BaseViewController
{
    #region VirtualKeyboard 虚拟键盘

    private LazyBox<UIView> _virtualKeyboardBarView = new();
    public UIView VirtualKeyboardView
    {
        get => _virtualKeyboardBarView.GetOrPut(() =>
        {
            var virtualKeyboardView = new UIView();

            virtualKeyboardView.Alpha = new nfloat(0.5);
            virtualKeyboardView.Hidden = true;
            virtualKeyboardView.Frame = CGRect.Empty;
            //virtualKeyboardView.Frame = new CGRect(
            //    0, 0, UIScreen.MainScreen.Bounds.Width.Value, 0);
            //virtualKeyboardView.BackgroundColor = UIColor.Green;

            return virtualKeyboardView;
        });
    }

    #endregion

}

