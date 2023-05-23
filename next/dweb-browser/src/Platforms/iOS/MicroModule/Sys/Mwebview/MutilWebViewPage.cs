using UIKit;
using CoreGraphics;
using System.Threading;
using DwebBrowser.Base;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.StatusBar;
using Microsoft.Maui.Controls;

namespace DwebBrowser.MicroService.Sys.Mwebview;

public partial class MultiWebViewController : BaseViewController
{
    #region 视图绑定

    public UIView webviewContainer = new();
    public State<CGRect> WebviewFrame = new(CGRect.Empty);

    async void BindWebViewItems()
    {
        // 设置视图控制器的背景颜色
        //webviewContainer.BackgroundColor = UIColor.Green;
        webviewContainer.BackgroundColor = UIColor.White;
        WebviewFrame.Set(webviewContainer.Frame);

        /// 视图绑定
        await foreach (var viewItemList in this.WebViewList.ToStream())
        {
            /// 移除所有的子视图
            foreach (var view in webviewContainer.Subviews.ToArray())
            {
                view.RemoveFromSuperview();
            }

            /// 注入子视图
            foreach (var viewItem in viewItemList)
            {
                WebviewFrame.OnChange += async (value, oldValue, _) =>
                {
                    viewItem.webView.Frame = value;
                    //Console.Log("viewItem.webView", "top: {0}, bottom: {1}, left: {2}, right; {3}",
                    //    value.Top.Value,
                    //    value.Bottom.Value,
                    //    value.Left.Value,
                    //    value.Right.Value);
                };
                Console.Log("foreach viewItem", "start");
                viewItem.webView.Frame = WebviewFrame.Get();
                webviewContainer.AddSubview(viewItem.webView);
                webviewContainer.AddSubview(StatusBarView);
                webviewContainer.AddSubview(NavigationBarView);
                webviewContainer.AddSubview(VirtualKeyboardView);
            }
        }
    }

    public override void ViewWillAppear(bool animated)
    {
        base.ViewWillAppear(animated);
    }

    public override void ViewDidLoad()
    {
        base.ViewDidLoad();
        /// webview 完全覆盖屏幕，包括安全区域
        webviewContainer.Frame = new CGRect(0, 0, View.Frame.Width, View.Frame.Height);

        View.AddSubview(webviewContainer);

        BindWebViewItems();
    }

    public override void ViewWillLayoutSubviews()
    {
        base.ViewWillLayoutSubviews();
    }

    public override void ViewDidLayoutSubviews()
    {
        base.ViewDidLayoutSubviews();
    }

    #endregion
}

