using UIKit;
using WebKit;
using DwebBrowser.MicroService.Sys.Js;
using DwebBrowser.Base;
using System.Collections.Generic;
using DwebBrowser.DWebView;

#nullable enable

namespace DwebBrowser.MicroService.Sys.Mwebview;

public partial class MultiWebViewController : BaseViewController
{
    static Debugger Console = new Debugger("MultiWebViewController");

    public Mmid Mmid { get; set; }
    public MultiWebViewNMM LocaleMM { get; set; }
    public MicroModule RemoteMM { get; set; }

    public MultiWebViewController(Mmid mmid, MultiWebViewNMM localeMM, MicroModule remoteMM)
    {
        Mmid = mmid;
        LocaleMM = localeMM;
        RemoteMM = remoteMM;
    }


    private static int s_webviewId_acc = 0;

    //private List<ViewItem> _webViewList = new();
    public State<List<ViewItem>> WebViewList = new(new List<ViewItem>());

    private Dictionary<Mmid, Ipc> _mIpcMap = new();

    public record ViewItem(string webviewId, DWebView.DWebView webView);

    public async Task<ViewItem> OpenWebViewAsync(string url, WKWebViewConfiguration? configuration = null)
    {
        var dwebview = await CreateDwebView(url, configuration);

        var viewItem = AppendWebViewAsItem(dwebview);
        Console.Log("openWebView", viewItem.webviewId);

        /// 提供窗口相关的行为
        dwebview.OnCreateWebView += async (args, _) =>
        {
            var item = await OpenWebViewAsync(args.navigationAction.Request.Url.AbsoluteString!, args.configuration);
            args.completionHandler(item.webView);
        };
        dwebview.OnClose += async (args, _) =>
        {
            await CloseWebViewAsync(viewItem.webviewId);
        };

        return viewItem;
    }

    public Task<DWebView.DWebView> CreateDwebView(string url, WKWebViewConfiguration? configuration = null)
    {
        return MainThread.InvokeOnMainThreadAsync(() =>
        {
            var dwebview = new DWebView.DWebView(null, LocaleMM, RemoteMM, new(url), configuration);
            return dwebview;
        });
    }

    public ViewItem AppendWebViewAsItem(DWebView.DWebView dwebview)
    {
        var webviewId = "#w" + Interlocked.Increment(ref s_webviewId_acc);

        return new ViewItem(webviewId, dwebview).Also(it =>
        {
            WebViewList.Update(list => list?.Add(it));
            // TODO: DWebView 还未实现 onCloseWindow
            //CloseWebViewAsync();

            Task.Run(async () =>
            {
                await (_OnWebViewOpen?.Emit(webviewId)).ForAwait();
            });
        });
    }

    public async Task<bool> CloseWebViewAsync(string webviewId)
    {
        var viewItem = WebViewList.Get().Find(viewItem => viewItem.webviewId == webviewId);

        if (viewItem is not null)
        {
            Console.Log("CloseWebView", viewItem.webviewId);

            if (WebViewList.Update((list) => list!.Remove(viewItem)))
            {
                viewItem.webView.Dispose();
                await (_OnWebViewClose?.Emit(webviewId)).ForAwait();
                return true;
            }
        }

        return false;
    }


    /// <summary>
    /// 移除webview所有列表
    /// </summary>
    public void DestroyWebView() => WebViewList.Update(list => list!.Clear());


    private event Signal<string>? _OnWebViewClose;
    private event Signal<string>? _OnWebViewOpen;

}

