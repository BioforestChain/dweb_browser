using System;
using System.Threading;
using CoreGraphics;
using DwebBrowser.Base;
using DwebBrowser.MicroService.Sys.Mwebview;
using UIKit;

namespace DwebBrowser.MicroService.Sys.Mwebview;

public partial class MultiWebViewController : BaseViewController
{
    #region 视图绑定

    UIView webviewContainer = new();

    async void BindWebViewItems()
    {
        // 设置视图控制器的背景颜色
        webviewContainer.BackgroundColor = UIColor.Green;


        /// 视图绑定
        await foreach (var viewItemList in this.WebViewList.ToStream())
        {
            /// 移除所有的子视图
            foreach (var view in webviewContainer.Subviews.ToArray())
            {
                view.RemoveFromSuperview();
            }
            /// 填充父级视图的宽高
            var webViewFrame = new CGRect(0, 0, webviewContainer.Frame.Width, webviewContainer.Frame.Height);
            /// 注入子视图
            foreach (var viewItem in viewItemList)
            {
                viewItem.webView.Frame = webViewFrame;
                webviewContainer.AddSubview(viewItem.webView);
            }
            //webviewContainer.AddDebugPoints();
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
    //public override bool PrefersStatusBarHidden()
    //{
    //    return false;
    //}

    //public override UIStatusBarStyle PreferredStatusBarStyle()
    //{
    //    return UIStatusBarStyle.LightContent;
    //}
    #endregion
}

