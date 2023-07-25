using System.Text.Json;
using DwebBrowser.Base;
using DwebBrowser.MicroService.Browser.Mwebview.DwebServiceWorker;
using DwebBrowser.MicroService.Browser.NativeUI;
using UIKit;
using WebKit;

#nullable enable

namespace DwebBrowser.MicroService.Browser.Mwebview;

public partial class MultiWebViewController : BaseViewController
{
    static readonly Debugger Console = new("MultiWebViewController");

    public Mmid Mmid { get; set; }
    public MultiWebViewNMM LocaleMM { get; set; }
    public MicroModule RemoteMM { get; set; }

    public MultiWebViewController(Mmid mmid, MultiWebViewNMM localeMM, MicroModule remoteMM)
    {
        Mmid = mmid;
        LocaleMM = localeMM;
        RemoteMM = remoteMM;

        var gesture = new UIScreenEdgePanGestureRecognizer();
        gesture.AddTarget(() => OnScreenEdgePan(gesture));

        //gesture.DelaysTouchesBegan = true;
        gesture.Edges = UIRectEdge.Left;
        gesture.CancelsTouchesInView = true;
        EdgeView.AddGestureRecognizer(gesture);

        #region WebViewList OnChange

        WebViewList.OnChange += async (value, oldValue, _) =>
        {
            var currentState = new Dictionary<string, Dictionary<string, object>>();
            value.ForEach(it =>
            {
                var dic = new Dictionary<string, object>
                {
                    { "webviewId", it.webviewId },
                    /// TODO 需要对webview的显示隐藏进行管理
                    { "isActivated", it.webView.Hidden },
                    { "url", it.webView.Url?.AbsoluteString ?? "" }
                };

                currentState.Add(it.webviewId, dic);
            });

            var ipc = await _mIpcMap.GetValueOrPutAsync(mmid, async () =>
            {
                var ipcForFromMM = await localeMM.ConnectAsync(mmid);
                ipcForFromMM!.OnEvent += async (Event, _, _) =>
                {
                    Console.Log("event", "name={0}, data={1}", Event.Name, Event.Data);
                };

                return ipcForFromMM;
            });

            await ipc.PostMessageAsync(IpcEvent.FromUtf8("state", JsonSerializer.Serialize(currentState)));
        };

        #endregion
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

    public override void ViewDidAppear(bool animated)
    {
        base.ViewDidAppear(animated);
        _ = Task.Run(async () =>
        {
            await ServiceWorker.EmitEventAsync(RemoteMM.Mmid, ServiceWorkerEvent.Resume.Event);
        }).NoThrow();
    }

    public override void ViewWillDisappear(bool animated)
    {
        base.ViewWillDisappear(animated);
        _ = Task.Run(async () =>
        {
            await ServiceWorker.EmitEventAsync(RemoteMM.Mmid, ServiceWorkerEvent.Pause.Event, "new Event(\"pause\")");
        }).NoThrow();
    }

    private static int s_webviewId_acc = 0;

    public State<List<ViewItem>> WebViewList = new(new List<ViewItem>());

    public ViewItem? LastViewOrNull { get => WebViewList.Get().LastOrDefault(); }

    public bool IsLastView(ViewItem viewItem) => WebViewList.Get().LastOrDefault() == viewItem;

    private readonly Dictionary<Mmid, Ipc> _mIpcMap = new();

    public record ViewItem(string webviewId, DWebView.DWebView webView, MultiWebViewController mwebviewController)
    {
        private readonly LazyBox<NativeUiController> _nativeUiController = new();
        public NativeUiController nativeUiController
        {
            get => _nativeUiController.GetOrPut(() => new NativeUiController(mwebviewController));
        }
    }

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

        return MainThread.InvokeOnMainThreadAsync(() => new ViewItem(webviewId, dwebview, this).Also(it =>
        {
            WebViewList.Update(list => list?.Add(it));
            dwebview.OnClose += async (_, _) =>
            {
                await CloseWebViewAsync(webviewId);
            };

            _ = Task.Run(async () =>
            {
                await (_WebViewOpenSignal.Emit(webviewId)).ForAwait();
            }).NoThrow();
        }));
    }

    public async Task<bool> CloseWebViewAsync(string webviewId)
    {
        var viewItem = WebViewList.Get().Find(viewItem => viewItem.webviewId == webviewId);

        if (viewItem is not null)
        {
            Console.Log("CloseWebView", viewItem.webviewId);

            if (WebViewList.Update((list) => list!.Remove(viewItem)))
            {
                viewItem.webView?.Dispose();
                await (_WebViewCloseSignal.Emit(webviewId)).ForAwait();
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
        WebViewList.Get().ForEach(viewItem =>
        {
            viewItem.webView?.Dispose();
        });

        WebViewList.Update(list => list!.Clear());

        return true;
    }


    private readonly HashSet<Signal<string>> _WebViewCloseSignal = new();
    private event Signal<string> _OnWebViewClose
    {
        add { if(value != null) lock (_WebViewCloseSignal) { _WebViewCloseSignal.Add(value); } }
        remove { lock (_WebViewCloseSignal) { _WebViewCloseSignal.Remove(value); } }
    }
    private readonly HashSet<Signal<string>> _WebViewOpenSignal = new();
    private event Signal<string> _OnWebViewOpen
    {
        add { if(value != null) lock (_WebViewOpenSignal) { _WebViewOpenSignal.Add(value); } }
        remove { lock (_WebViewOpenSignal) { _WebViewOpenSignal.Remove(value); } }
    }

}

