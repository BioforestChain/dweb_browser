using UIKit;
using CoreGraphics;
using DwebBrowser.Base;
using DwebBrowser.MicroService.Browser.NativeUI.Base;

namespace DwebBrowser.MicroService.Browser.Mwebview;

public partial class MultiWebViewController : BaseViewController
{
    #region HomeIndicator 底部导航

    private LazyBox<UIView> _navigationBarView = new();
    /// HomeIndicator 的 Style 跟随 StatusBar 的 Style 变化
    public UIView NavigationBarView
    {
        get => _navigationBarView.GetOrPut(() =>
        {
            var app = UIApplication.SharedApplication;
            var safeAreaInsets = app.KeyWindow.SafeAreaInsets;
            var colorJson = app.StatusBarStyle.ToColor();
            var navigationBarView = new UIView();

            //navigationBarView.BackgroundColor = UIColor.FromRGBA(colorJson.red, colorJson.green, colorJson.blue, colorJson.alpha);
            navigationBarView.BackgroundColor = UIColor.Yellow;
            navigationBarView.Alpha = new nfloat(0.5);
            navigationBarView.Hidden = false;
            navigationBarView.Frame = new CGRect(
                0,
                UIScreen.MainScreen.Bounds.GetMaxY().Value - safeAreaInsets.Bottom.Value,
                UIScreen.MainScreen.Bounds.Width.Value,
                safeAreaInsets.Bottom.Value);

            return navigationBarView;
        });
    }

    public override bool PrefersHomeIndicatorAutoHidden => NavigationBarView.Hidden;

    public override void SetNeedsUpdateOfHomeIndicatorAutoHidden()
    {
        base.SetNeedsUpdateOfHomeIndicatorAutoHidden();
    }

    #endregion

}

