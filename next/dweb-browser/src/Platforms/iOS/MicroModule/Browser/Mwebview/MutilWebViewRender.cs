using DwebBrowser.MicroService.Browser.Desk;
using UIKit;
using CoreGraphics;

namespace DwebBrowser.MicroService.Browser.Mwebview;

public partial class MultiWebViewController
{
    private UIView WebviewContainer = new();

    #region 视图绑定
    // 用于触发右滑
    public UIView EdgeView = new();

    Task Render(WindowRenderScope windowRenderScope, WindowController win) => MainThread.InvokeOnMainThreadAsync(() =>
    {
        var deskAppUIView = new DeskAppUIView(win);
        deskAppUIView.Layer.ZPosition = win.State.ZIndex;
        var bounds = win.State.Bounds;

        /// 移除所有的子视图
        foreach (var view in WebviewContainer.Subviews)
        {
            view.RemoveFromSuperview();
        }

        /// 注入子视图
        foreach (var viewItem in WebViewList)
        {
            WebviewContainer.AddSubview(viewItem.webView);
            viewItem.webView.AutoResize("webview", WebviewContainer);
        }

        deskAppUIView.Render(WebviewContainer, windowRenderScope);
    });

    #endregion
}

