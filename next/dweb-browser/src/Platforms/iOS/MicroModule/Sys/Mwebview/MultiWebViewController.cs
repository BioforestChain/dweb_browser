using UIKit;
using DwebBrowser.MicroService.Sys.Js;
using DwebBrowser.Base;

#nullable enable

namespace DwebBrowser.MicroService.Sys.Mwebview;

public class MultiWebViewController : BaseViewController
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

    private static int s_webviewId_acc = 1;

    private List<ViewItem> _webViewList = new();

    private Dictionary<Mmid, Ipc> _mIpcMap = new();

    public record ViewItem(string webviewId, DWebView.DWebView webView);

    public async Task<ViewItem> OpenWebViewAsync(string url)
    {
        var dwebview = CreateDwebView(url);
        await dwebview.LoadURL(dwebview.Url?.AbsoluteString ?? "");
        var viewItem = AppendWebViewAsItem(dwebview);
        Console.Log("openWebView", viewItem.webviewId);
        await _updateStateHookAsync("openWebView");

        return viewItem;
    }

    public DWebView.DWebView CreateDwebView(string url)
    {
        return new(null, LocaleMM, RemoteMM, new(url), null);
    }

    public ViewItem AppendWebViewAsItem(DWebView.DWebView dwebview)
    {
        var webviewId = "#w" + Interlocked.Exchange(ref s_webviewId_acc, Interlocked.Increment(ref s_webviewId_acc));
        
        return new ViewItem(webviewId, dwebview).Also(it =>
        {
            _webViewList.Add(it);
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
        var viewItem = _webViewList.Find(viewItem => viewItem.webviewId == webviewId);

        if (viewItem is not null)
        {
            Console.Log("CloseWebView", viewItem.webviewId);
            var _bool = _webViewList.Remove(viewItem);

            if (_bool)
            {
                await _updateStateHookAsync("closeWebView");
            }

            //await MainThread.InvokeOnMainThreadAsync(() => viewItem.webView.)
            await (_OnWebViewClose?.Emit(webviewId)).ForAwait();

            return true;
        }

        return false;
    }
        

    /// <summary>
    /// 移除webview所有列表
    /// </summary>
    public void DestroyWebView() => _webViewList.Clear();

    // TODO _updateStateHook未完成
    private async Task _updateStateHookAsync(string handler)
    {
        Console.Log("_updateStateHook " + handler, "localeMM: {0} mmid: {1} {2}", LocaleMM.Mmid, Mmid, _webViewList.Count);
        var ipc = await _mIpcMap.GetValueOrPutAsync(Mmid, async () =>
        {
            var connectResult = await LocaleMM.ConnectAsync(Mmid);
            var ipc = connectResult.IpcForFromMM;
            ipc.OnEvent += async (Event, _, _) =>
            {
                Console.Log("event", "name={0}, data={1}", Event.Name, Event.Data);
            };
            return ipc;
        });

        await ipc.PostMessageAsync(IpcEvent.FromUtf8("state", ""));
    }

    private event Signal<string>? _OnWebViewClose;
    private event Signal<string>? _OnWebViewOpen;
}

