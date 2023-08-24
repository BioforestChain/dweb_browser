namespace DwebBrowser.MicroService.Browser.Mwebview.DwebServiceWorker;

public static class ServiceWorker
{
    private const string DWEB_SERVICE_WORKER = "__app_upgrade_watcher_kit__";

    public static async Task<bool> EmitEventAsync(Mmid mmid, string eventName, string data = "")
    {
        var controller = MultiWebViewNMM.GetCurrentWebViewController(mmid);
        var payload = string.Format("new Event({0})", eventName);

        // progress,fetch,onFetch为自定义构造返回
        if (eventName == DownloadControllerEvent.Progress.Event)
        {
            payload = data;
        }

        await MainThread.InvokeOnMainThreadAsync(() =>
        {
            controller.WebViewList.LastOrNull()?.webView.EvaluateAsyncJavascriptCode($$"""
            new Promise((resolve,reject)=>{
                try{
                    const listeners = {{DWEB_SERVICE_WORKER}}._listeners["{{eventName}}"];
                    if (listeners.length !== 0) {
                      listeners.forEach(listener => listener({{payload}}));
                      resolve(true)
                    }
                    resolve(false)
                }catch(err){console.log(err);resolve(false)}
            })
            """.Trim());
        });

        return true;
    }
}

public record ServiceWorkerEvent(string Event)
{
    // 更新或重启的时候触发
    public static readonly ServiceWorkerEvent UpdateFound = new("updatefound");
    public static readonly ServiceWorkerEvent Fetch = new("fetch");
    public static readonly ServiceWorkerEvent OnFetch = new("onFetch");
    public static readonly ServiceWorkerEvent Pause = new("pause");
    public static readonly ServiceWorkerEvent Resume = new("resume");
}

public record DownloadControllerEvent(string Event)
{
    // 监听启动
    public static readonly DownloadControllerEvent Start = new("start");
    // 进度每秒触发一次
    public static readonly DownloadControllerEvent Progress = new("progress");
    // 结束
    public static readonly DownloadControllerEvent End = new("end");
    // 取消
    public static readonly DownloadControllerEvent Cancel = new("cancel");
    // 暂停
    public static readonly DownloadControllerEvent Pause = new("pause");
}
