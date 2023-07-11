using CoreGraphics;
using DwebBrowser.Base;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Mwebview;

public partial class MultiWebViewController : BaseViewController
{
    #region VirtualKeyboard 虚拟键盘

    private readonly LazyBox<UIView> _virtualKeyboardBarView = new();
    public UIView VirtualKeyboardView
    {
        get => _virtualKeyboardBarView.GetOrPut(() =>
        {
            var virtualKeyboardView = new UIView
            {
                Alpha = new nfloat(0.5),
                Hidden = true,
                Frame = CGRect.Empty
            };
            //virtualKeyboardView.Frame = new CGRect(
            //    0, 0, UIScreen.MainScreen.Bounds.Width.Value, 0);
            //virtualKeyboardView.BackgroundColor = UIColor.Green;

            return virtualKeyboardView;
        });
    }

    #endregion

}

