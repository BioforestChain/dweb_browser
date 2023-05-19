using System;
using System.Threading;
using CoreGraphics;
using DwebBrowser.Base;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.StatusBar;
using UIKit;

namespace DwebBrowser.MicroService.Sys.Mwebview;

public partial class MultiWebViewController : BaseViewController
{
    #region 视图绑定

    public UIView webviewContainer = new();

    async void BindWebViewItems()
    {
        // 设置视图控制器的背景颜色
        //webviewContainer.BackgroundColor = UIColor.Green;
        webviewContainer.BackgroundColor = UIColor.White;

        /// 视图绑定
        await foreach (var viewItemList in this.WebViewList.ToStream())
        {
            /// 移除所有的子视图
            foreach (var view in webviewContainer.Subviews.ToArray())
            {
                view.RemoveFromSuperview();
            }

            Console.Log("BindWebViewItems", "viewItemList");

            ///// 注入NavigationBar
            //NavigationBarView.Frame = NavigationBarVisible
            //    ? new CGRect(
            //        0,
            //        webviewContainer.Frame.GetMaxY().Value - webviewContainer.SafeAreaInsets.Bottom.Value,
            //        webviewContainer.Frame.Width.Value,
            //        webviewContainer.SafeAreaInsets.Bottom.Value)
            //    : CGRect.Empty;
            //NavigationBarView.BackgroundColor = UIColor.Red;

            /// 填充父级视图的宽高

            //var webViewFrame = new CGRect(0, 0, webviewContainer.Frame.Width, webviewContainer.Frame.Height);

            /// 注入子视图
            foreach (var viewItem in viewItemList)
            {
                //Console.Log("foreach viewItem", "start");
                //var webViewHeight = webviewContainer.Frame.Height.Value
                //- StatusBarView.Frame.Height.Value;
                ////- NavigationBarView.Frame.Height.Value;
                //var webViewFrame = new CGRect(
                //    0,
                //    StatusBarView.Frame.Height.Value,
                //    webviewContainer.Frame.Width.Value,
                //    webViewHeight);
                //viewItem.webView.Frame = webViewFrame;
                //webviewContainer.AddSubview(viewItem.webView);
                //var insets = webviewContainer.SafeAreaInsets;
                //Console.Log("BindWebViewItems", "top: {0}, bottom: {1}, left: {2}, right: {3}",
                //    insets.Top.Value, insets.Bottom.Value, insets.Left.Value, insets.Right.Value);
                ////webviewContainer.AddSubview(StatusBarView);
                //Console.Log("foreach viewItem", "end");
                try
                {
                    Console.Log("foreach viewItem", "start");
                    var webViewHeight = webviewContainer.Frame.Height.Value
                    - StatusBarView.Frame.Height.Value;
                    //- NavigationBarView.Frame.Height.Value;
                    var webViewFrame = new CGRect(
                        0,
                        StatusBarView.Frame.Height.Value,
                        webviewContainer.Frame.Width.Value,
                        webViewHeight - NavigationBarView.Frame.Height.Value - VirtualKeyboardView.Frame.Height.Value);
                    viewItem.webView.Frame = webViewFrame;
                    webviewContainer.AddSubview(viewItem.webView);
                    var insets = webviewContainer.SafeAreaInsets;
                    Console.Log("BindWebViewItems", "top: {0}, bottom: {1}, left: {2}, right: {3}",
                        insets.Top.Value, insets.Bottom.Value, insets.Left.Value, insets.Right.Value);
                    webviewContainer.AddSubview(StatusBarView);
                    Console.Log("foreach viewItem", "top: {0}, bottom: {1}, left: {2}, right: {3}",
                        StatusBarView.Frame.Top.Value,
                        StatusBarView.Frame.Bottom.Value,
                        StatusBarView.Frame.Left.Value,
                        StatusBarView.Frame.Right.Value);
                    webviewContainer.AddSubview(NavigationBarView);
                    webviewContainer.AddSubview(VirtualKeyboardView);
                }
                catch (Exception e)
                {
                    Console.Log("foreach Exception", e.Message);
                }
            }
            //webviewContainer.AddSubview(StatusBarView);
            //webviewContainer.AddSubview(NavigationBarView);
            //webviewContainer.AddDebugPoints();
            //Console.Log("UIScreen", "y: {0}, height: {1}, width: {2}, x: {3}",
            //    UIScreen.MainScreen.Bounds.GetMaxY().Value,
            //    UIScreen.MainScreen.Bounds.Height.Value,
            //    UIScreen.MainScreen.Bounds.Width.Value,
            //    UIScreen.MainScreen.Bounds.GetMaxX().Value);

            //var statusView = webviewContainer.ViewWithTag(new nint(1000));
            //Console.Log("statusView", "top: {0}, bottom: {1}, left: {2}, right: {3}",
            //    statusView.Frame.Top.Value, statusView.Frame.Bottom.Value, statusView.Frame.Left.Value, statusView.Frame.Right.Value);
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

