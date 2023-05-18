using UIKit;
using CoreGraphics;
using DwebBrowser.Base;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.Base;

namespace DwebBrowser.MicroService.Sys.Mwebview;

public partial class MultiWebViewController : BaseViewController
{
    #region StatusBar 状态栏

    public UIView StatusBarView
    {
        get => _statusBarView.GetOrPut(() =>
        {
            var app = UIApplication.SharedApplication;
            var colorJson = app.StatusBarStyle.ToColor();
            var statusBarView = new UIView();

            //statusBarView.BackgroundColor = UIColor.FromRGBA(colorJson.red, colorJson.green, colorJson.blue, colorJson.alpha);
            statusBarView.BackgroundColor = UIColor.Red;
            statusBarView.Hidden = app.StatusBarHidden;
            statusBarView.Alpha = new nfloat(0.5);
            statusBarView.Frame = app.StatusBarFrame;

            return statusBarView;
        });
    }
    private LazyBox<UIView> _statusBarView = new();

    public BarStyle StatusBarStyle = BarStyle.Default;

    public override bool PrefersStatusBarHidden()
    {
        Console.Log("PrefersStatusBarHidden", "visible: {0}", StatusBarView.Hidden);
        return StatusBarView.Hidden;
    }

    public override UIStatusBarStyle PreferredStatusBarStyle()
    {
        return StatusBarStyle.ToUIStatusBarStyle();
    }

    #endregion
}

