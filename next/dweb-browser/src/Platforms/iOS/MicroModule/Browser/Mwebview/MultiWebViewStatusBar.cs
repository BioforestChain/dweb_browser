using DwebBrowser.Base;
using DwebBrowser.MicroService.Browser.NativeUI.Base;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Mwebview;

public partial class MultiWebViewController : BaseViewController
{
    #region StatusBar 状态栏

    public UIView StatusBarView
    {
        get => _statusBarView.GetOrPut(() =>
        {
            var app = UIApplication.SharedApplication;
            var colorJson = app.StatusBarStyle.ToColor();
            var statusBarView = new UIView
            {
                //statusBarView.BackgroundColor = UIColor.FromRGBA(colorJson.red, colorJson.green, colorJson.blue, colorJson.alpha);
                BackgroundColor = UIColor.Red,
                Hidden = app.StatusBarHidden,
                Alpha = new nfloat(0.5),
                Frame = app.KeyWindow.WindowScene.StatusBarManager.StatusBarFrame
            };

            return statusBarView;
        });
    }
    private readonly LazyBox<UIView> _statusBarView = new();

    public BarStyle StatusBarStyle = BarStyle.Default;

    public override bool PrefersStatusBarHidden() => StatusBarView.Hidden;

    public override UIStatusBarStyle PreferredStatusBarStyle()
    {
        return StatusBarStyle.ToUIStatusBarStyle();
    }

    #endregion
}

