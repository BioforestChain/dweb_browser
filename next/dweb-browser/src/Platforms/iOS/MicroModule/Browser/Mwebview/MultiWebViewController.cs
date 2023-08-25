using System.Text.Json;
using UIKit;
using WebKit;
using CoreGraphics;

#nullable enable

namespace DwebBrowser.MicroService.Browser.Mwebview;

public partial class MultiWebViewController
{
    static readonly Debugger Console = new("MultiWebViewController");

    public WindowController Win { get; set; }
    public Ipc Ipc { get; init; }
    public MicroModule LocaleMM { get; set; }
    public MicroModule RemoteMM { get; set; }

    public MultiWebViewController(WindowController win, Ipc ipc, MicroModule localeMM, MicroModule remoteMM)
    {
        Win = win;
        Ipc = ipc;
        LocaleMM = localeMM;
        RemoteMM = remoteMM;

        var gesture = new UIScreenEdgePanGestureRecognizer();
        gesture.AddTarget(() => OnScreenEdgePan(gesture));
        gesture.Edges = UIRectEdge.Left;
        gesture.CancelsTouchesInView = true;
        EdgeView.AddGestureRecognizer(gesture);

        WebViewList.OnChangeAdd(async (_, _) =>
        {
            await UpdateStateHook();
        });

        WindowAdapterManager.Instance.RenderProviders.TryAdd(win.Id, Render);

        /// 窗口销毁的时候
        Win.OnClose.OnListener += async (_, _) =>
        {
            /// 移除渲染适配器
            WindowAdapterManager.Instance.RenderProviders.Remove(win.Id, out _);

            /// 清除释放所有的 webview
            foreach (var item in WebViewList)
            {
                await CloseWebViewAsync(item.webviewId);
            }
        };

        /// ipc 断开的时候，强制关闭窗口
        ipc.OnClose += async (_) =>
        {
            await Win.Close(true);
        };
    }

    UIGestureRecognizerState preState = UIGestureRecognizerState.Ended;
    async void OnScreenEdgePan(UIScreenEdgePanGestureRecognizer pan)
    {
        //pan.ShouldRecognizeSimultaneously = new UIGesturesProbe();
        if (pan.State != preState)
        {
            preState = pan.State;
            Console.Log("OnScreenEdgePan", "state: {0}", pan.State);
        }

        if (pan.State == UIGestureRecognizerState.Ended)
        {
            var LastViewOrNull = WebViewList.LastOrNull();
            if (LastViewOrNull is not null)
            {
                if (LastViewOrNull.webView.CanGoBack)
                {
                    LastViewOrNull.webView.GoBack();
                }
                else
                {
                    await CloseWebViewAsync(LastViewOrNull.webviewId);
                }
            }
        }
    }

    public readonly ChangeableList<ViewItem> WebViewList = new();

    internal record ViewItemState(string webviewId, bool isActivated, string url);

    public async Task UpdateStateHook()
    {
        var currentState = new Dictionary<string, ViewItemState>();
        WebViewList.ForEach(it =>
        {
            var viewItemState = new ViewItemState(it.webviewId, WebviewContainer.Hidden, it.webView.Url?.AbsoluteString ?? "");
            currentState.Add(it.webviewId, viewItemState);
        });

        await Ipc.PostMessageAsync(IpcEvent.FromUtf8(EIpcEvent.State.Event, JsonSerializer.Serialize(currentState)));
    }

    private static int s_webviewId_acc = 0;

    private readonly Dictionary<Mmid, Ipc> _mIpcMap = new();

    public record ViewItem(
        string webviewId,
        DWebView.DWebView webView,
        MultiWebViewController mwebviewController,
        WindowController win);

    public async Task<ViewItem> OpenWebViewAsync(string url, WKWebViewConfiguration? configuration = null)
    {
        var dwebview = await CreateDwebView(url, configuration);

        var viewItem = await AppendWebViewAsItemAsync(dwebview);
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

    public Task<ViewItem> AppendWebViewAsItemAsync(DWebView.DWebView dwebview)
    {
        var webviewId = "#w" + Interlocked.Increment(ref s_webviewId_acc);

        return MainThread.InvokeOnMainThreadAsync(() => new ViewItem(webviewId, dwebview, this, Win).Also(it =>
        {
            WebViewList.Add(it);
            dwebview.OnClose += async (_, _) =>
            {
                await CloseWebViewAsync(webviewId);
            };

            _ = Task.Run(async () =>
            {
                await _WebViewOpenSignal.Emit(webviewId).ForAwait();
            }).NoThrow();
        }));
    }

    public async Task<bool> CloseWebViewAsync(string webviewId)
    {
        var viewItem = WebViewList.Find(viewItem => viewItem.webviewId == webviewId);

        if (viewItem is not null)
        {
            if (WebViewList.Remove(viewItem))
            {
                await MainThread.InvokeOnMainThreadAsync(async () =>
                {
                    viewItem.webView?.Dispose();
                });
                await _WebViewCloseSignal.Emit(webviewId).ForAwait();

                return true;
            }
        }

        return false;
    }

    /// <summary>
    /// 移除webview所有列表
    /// </summary>
    public bool DestroyWebView()
    {
        MainThread.InvokeOnMainThreadAsync(() =>
        {
            WebViewList.ForEach(viewItem =>
            {
                viewItem.webView?.Dispose();
            });
        });

        WebViewList.Clear();

        return true;
    }


    private readonly HashSet<Signal<string>> _WebViewCloseSignal = new();
    private event Signal<string> _OnWebViewClose
    {
        add { if (value != null) lock (_WebViewCloseSignal) { _WebViewCloseSignal.Add(value); } }
        remove { lock (_WebViewCloseSignal) { _WebViewCloseSignal.Remove(value); } }
    }
    private readonly HashSet<Signal<string>> _WebViewOpenSignal = new();
    private event Signal<string> _OnWebViewOpen
    {
        add { if (value != null) lock (_WebViewOpenSignal) { _WebViewOpenSignal.Add(value); } }
        remove { lock (_WebViewOpenSignal) { _WebViewOpenSignal.Remove(value); } }
    }

}

