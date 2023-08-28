using DwebBrowser.MicroService.Browser.Desk;
using DwebBrowserFramework;
using Foundation;

namespace DwebBrowser.MicroService.Browser.Jmm;

public class JmmController
{
    static readonly Debugger Console = new("JmmController");
    private JmmNMM JmmNmm { get; init; }
    public WindowController Win { get; init; }

    public JmmController(JmmNMM jmmNMM, WindowController win)
    {
        JmmNmm = jmmNMM;
        Win = win;
    }

    public async Task OpenDownloadPageAsync(JmmAppDownloadManifest jmmAppDownloadManifest)
    {
        var data = NSData.FromString(jmmAppDownloadManifest.ToJson(), NSStringEncoding.UTF8);
        var manager = new DownloadAppManager(data);

        // 点击下载
        manager.ClickButtonActionWithCallback(async d =>
        {
            switch (d.ToString())
            {
                case "download":
                    if (jmmAppDownloadManifest.DownloadStatus == DownloadStatus.NewVersion)
                    {
                        await JmmNmm.BootstrapContext.Dns.Close(jmmAppDownloadManifest.Id);
                    }
                    var jmmDownload = JmmDwebService.Add(jmmAppDownloadManifest,
                         manager.OnDownloadChangeWithDownloadStatus,
                         manager.OnListenProgressWithProgress);

                    JmmDwebService.DownloadComplete.OnListener += async (self) =>
                    {
                        new JsMicroModule(new JsMMMetadata(jmmAppDownloadManifest)).Also((jsMicroModule) =>
                        {
                            JmmNmm.BootstrapContext.Dns.Install(jsMicroModule);
                        });
                        JmmDwebService.DownloadComplete.OnListener -= self;
                    };

                    JmmDwebService.Start();
                    
                    break;
                case "open":
                    Console.Log("open", jmmAppDownloadManifest.ToJson());
                    await OpenApp(jmmAppDownloadManifest.Id);

                    break;
                case "back":
                    //vc.PopViewController(true);
                    /// TODO: 关闭页面
                    
                    break;
                default:
                    break;
            }
        });

        /// 提供渲染适配
        WindowAdapterManager.Instance.RenderProviders.TryAdd(Win.Id, (windowRenderScope, win) =>
            MainThread.InvokeOnMainThreadAsync(() =>
            {
                var deskAppUIView = new DeskAppUIView(win);
                deskAppUIView.Layer.ZPosition = win.State.ZIndex;
                deskAppUIView.Render(manager.DownloadView, windowRenderScope);
            }));

        /// 窗口销毁的时候
        Win.OnClose.OnListener += async (_, _) =>
        {
            /// 移除渲染适配器
            WindowAdapterManager.Instance.RenderProviders.Remove(Win.Id, out _);
        };
    }

    public Mutex Mutex = new();

    public async Task OpenApp(Mmid mmid)
    {
        Mutex.WaitOne();

        var connectResult = await JmmNmm.BootstrapContext.Dns.ConnectAsync(mmid);
        Console.Log("openApp", "postMessage ==> activity {0}, {1}", mmid, connectResult.IpcForFromMM.Remote.Mmid);
        await connectResult.IpcForFromMM.PostMessageAsync(IpcEvent.FromUtf8(EIpcEvent.Activity.Event, ""));

        var deskConnectResult = await JmmNmm.BootstrapContext.Dns.ConnectAsync("desk.browser.dweb");
        Console.Log("openApp", "postMessage ==> activity {0}, {1}", mmid, deskConnectResult.IpcForFromMM.Remote.Mmid);
        await deskConnectResult.IpcForFromMM.PostMessageAsync(IpcEvent.FromUtf8(EIpcEvent.Activity.Event, ""));

        Mutex.ReleaseMutex();
    }

    public async Task CloseApp(Mmid mmid)
    {
        Console.Log("closeApp", $"mmid={mmid}");
        await JmmNmm.BootstrapContext.Dns.Close(mmid);
    }
}

