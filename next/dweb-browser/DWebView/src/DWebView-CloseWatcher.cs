using UIKit;
using WebKit;
using DwebBrowser.Helper;
using System;

namespace DwebBrowser.DWebView;

public class CloseWatcher
{
    private static int s_acc_id = 0;
    private static readonly string JS_POLYFILL_KIT = "__native_close_watcher_kit__";

    private DWebView _webview { get; init; }
    public readonly HashSet<string> Consuming = new();

    public CloseWatcher(DWebView webview)
    {
        _webview = webview;
    }

    public void RegistryToken(string consumeToken)
    {
        Consuming.Add(consumeToken);
    }

    public void TryClose(string id)
    {
        var watcher = _watchers.FirstOrDefault(w => w.Id == id);
        if (watcher != null)
        {
            Task.Run(() => CloseAsync(watcher));
        }
    }

    private List<Watcher> _watchers = new();

    public class Watcher
    {
        public string Id = Interlocked.Increment(ref s_acc_id).ToString();
        private long _destroy = 0;
        private Mutex _closeMutex = new Mutex(false);
        private DWebView _webview { get; init; }

        public Watcher(DWebView webview)
        {
            _webview = webview;
        }

        public async Task<bool> TryCloseAsync()
        {
            _closeMutex.WaitOne();

            if (Interlocked.Read(ref _destroy) == 1)
            {
                return false;
            }

            await _webview.InvokeOnMainThreadAsync(async () =>
            {
                await _webview.EvaluateJavaScriptAsync(
                    JS_POLYFILL_KIT + "._watchers?.get('" + Id + "')?.dispatchEvent(new CloseEvent('close'));");
            });

            _closeMutex.ReleaseMutex();

            return destroy();
        }

        public bool destroy() => Interlocked.CompareExchange(ref _destroy, 0, 1) == 0;
    }

    /// <summary>
    /// 申请一个 CloseWatcher
    /// </summary>
    public Watcher Apply(bool isUserGestrue)
    {
        if (isUserGestrue || _watchers.Count == 0)
        {
            _watchers.Add(new Watcher(_webview));
        }

        return _watchers.Last();
    }

    public async void ResolveToken(string consumeToken, Watcher watcher)
    {
        await _webview.InvokeOnMainThreadAsync(async () =>
        {
            await _webview.EvaluateJavaScriptAsync(
                JS_POLYFILL_KIT + "._tasks?.get('" + consumeToken + "')?.('" + watcher.Id + "');");
        });
    }

    /// <summary>
    /// 现在是否有 CloseWatcher 在等待被关闭
    /// </summary>
    public bool CanClose { get => _watchers.Count > 0; }

    /// <summary>
    /// 关闭指定的 CloseWatcher
    /// </summary>
    public async Task<bool> CloseAsync(Watcher? watcher = null)
    {
        if (watcher is null)
        {
            watcher = _watchers.Last();
        }

        if (await watcher.TryCloseAsync())
        {
            return _watchers.Remove(watcher);
        }

        return false;
    }
}

public partial class DWebView : WKWebView
{
    private LazyBox<WKScriptMessageHandler> _webCloseWatcherMessageHandler = new();
    public WKScriptMessageHandler CloseWatcherMessageHanlder
    {
        get => _webCloseWatcherMessageHandler.GetOrPut(() => new WebCloseWatcherMessageHanlder(this));
    }

    private LazyBox<CloseWatcher> _closeWatcherController = new();
    public CloseWatcher CloseWatcherController
    {
        get => _closeWatcherController.GetOrPut(() => new(this));
    }

    internal class WebCloseWatcherMessageHanlder : WKScriptMessageHandler
    {
        private DWebView _dWebView { get; init; }
        public WebCloseWatcherMessageHanlder(DWebView dWebView)
        {
            _dWebView = dWebView;
        }

        [Export("userContentController:didReceiveScriptMessage:")]
        public override async void DidReceiveScriptMessage(WKUserContentController userContentController, WKScriptMessage messageEvent)
        {
            var message = messageEvent.Body;
            var consumeToken = (string)(NSString)message.ValueForKey(new NSString("token"));
            var id = (string)(NSString)message.ValueForKey(new NSString("id"));

            if (!string.IsNullOrEmpty(consumeToken))
            {
                _dWebView.CloseWatcherController.RegistryToken(consumeToken);
            }
            else if (!string.IsNullOrEmpty(id))
            {
                _dWebView.CloseWatcherController.TryClose(id);
            }
        }
    }

    public override bool CanGoBack
    {
        get
        {
            return CloseWatcherController.CanClose || base.CanGoBack;
        }
    }
    public override WKNavigation? GoBack()
    {
        if (CloseWatcherController.CanClose) {
            CloseWatcherController.CloseAsync().NoThrow();
            return null;
        }
        return base.GoBack();
    }

}

