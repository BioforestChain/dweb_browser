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
            /// 注入子视图
            foreach (var viewItem in viewItemList)
            {
                viewItem.webView.Frame = View.Frame;
                webviewContainer.AddSubview(viewItem.webView);
            }
        }
    }


    public override void ViewDidLoad()
    {
        base.ViewDidLoad();
        webviewContainer.Frame = View.Frame;
        View.AddSubview(webviewContainer);

        BindWebViewItems();
    }
    #endregion
}

