using DwebBrowser.Base;
using DwebBrowser.MicroService.Browser.Desk;
using DwebBrowserFramework;
using Foundation;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Jmm;

public class JmmController : BaseViewController
{
    static readonly Debugger Console = new("JmmController");
    private JmmNMM _jmmNMM { get; init; }

    public JmmController(JmmNMM jmmNMM)
    {
        _jmmNMM = jmmNMM;
    }

    public Task OpenDownloadPageAsync(JmmAppDownloadManifest jmmAppDownloadManifest) =>
        MainThread.InvokeOnMainThreadAsync(async () =>
        {
            var data = NSData.FromString(jmmAppDownloadManifest.ToJson(), NSStringEncoding.UTF8);
            var vc = await IOSNativeMicroModule.RootViewController.WaitPromiseAsync();
            var manager = new DownloadAppManager(data);

            manager.DownloadView.Frame = UIScreen.MainScreen.Bounds;

            // 移除所有子视图
            //foreach (var subview in View.Subviews)
            //{
            //    subview.RemoveFromSuperview();
            //}

            //View.AddSubview(manager.DownloadView);
            DeskNMM.DeskController.InsertSubviewBelow(manager.DownloadView);

            // 无法push同一个UIViewController的实例两次
            //var viewControllersList = vc.ViewControllers?.ToList();
            //var index = viewControllersList.FindIndex(uvc => uvc == this);
            //if (index >= 0)
            //{
            //    // 不是当前controller时，推到最新
            //    //if (index != vc.ViewControllers!.Length - 1)
            //    //{
            //    //    vc.PopToViewController(this, true);
            //    //}
            //    viewControllersList.RemoveAt(index);
            //    vc.PushViewController(this, true);
            //}
            //else
            //{
            //    vc.PushViewController(this, true);
            //}

            // 点击下载
            manager.ClickButtonActionWithCallback(async d =>
            {
                switch (d.ToString())
                {
                    case "download":
                        if (jmmAppDownloadManifest.DownloadStatus == DownloadStatus.NewVersion)
                        {
                            await _jmmNMM.BootstrapContext.Dns.Close(jmmAppDownloadManifest.Id);
                        }
                        var jmmDownload = JmmDwebService.Add(jmmAppDownloadManifest,
                             manager.OnDownloadChangeWithDownloadStatus,
                             manager.OnListenProgressWithProgress);

                        JmmDwebService.Start();
                        break;
                    case "open":
                        Console.Log("open", jmmAppDownloadManifest.ToJson());
                        new JsMicroModule(new JsMMMetadata(jmmAppDownloadManifest)).Also((jsMicroModule) =>
                        {
                            _jmmNMM.BootstrapContext.Dns.Install(jsMicroModule);
                        });

                        await OpenApp(jmmAppDownloadManifest.Id);

                        break;
                    case "back":
                        vc.PopViewController(true);
                        break;
                    default:
                        break;
                }
            });
        });

    public async Task OpenApp(Mmid mmid)
    {
        var connectResult = await _jmmNMM.BootstrapContext.Dns.ConnectAsync(mmid);

        Console.Log("openApp", "postMessage ==> activity {0}, {1}", mmid, connectResult.IpcForFromMM.Remote.Mmid);
        await connectResult.IpcForFromMM.PostMessageAsync(IpcEvent.FromUtf8(EIpcEvent.Activity.Event, ""));
    }
}

