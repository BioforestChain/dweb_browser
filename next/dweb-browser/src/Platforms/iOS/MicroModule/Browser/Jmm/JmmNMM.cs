using UIKit;
using Foundation;
using BrowserFramework;

namespace DwebBrowser.MicroService.Browser.Jmm;

public class JmmNMM : NativeMicroModule
{
    static Debugger Console = new("JmmNMM");
    private static Dictionary<Mmid, JsMicroModule> s_apps = new();
    private static readonly List<JmmController> s_controllerList = new();
    public static Dictionary<Mmid, JsMicroModule> GetAndUpdateJmmNmmApps() => s_apps;

    public override List<Dweb_DeepLink> Dweb_deeplinks { get; init; } = new() { "dweb:install" };

    /// <summary>
    /// 获取当前App的数据配置
    /// </summary>
    /// <param name="mmid"></param>
    /// <returns></returns>
    public static JmmMetadata? GetBfsMetaData(Mmid mmid) => s_apps.GetValueOrDefault(mmid)?.Metadata;

    public static JmmController JmmController
    {
        get => s_controllerList.FirstOrDefault();
    }

    public JmmNMM() : base("jmm.browser.dweb")
    {
        s_controllerList.Add(new(this));
    }

    private void _recoverAppData()
    {
        // 启动的时候，从数据库中恢复 s_apps 对象
        _ = Task.Run(async () =>
        {
            foreach (var entry in JmmMetadataDB.GetJmmMetadataEnumerator())
            {
                s_apps.GetValueOrPut(entry.Key, () =>
                    new JsMicroModule(entry.Value).Also(jsMicroModule =>
                    {
                        try
                        {
                            BootstrapContext.Dns.Install(jsMicroModule);
                        }
                        catch { }
                    }));
            }
        }).NoThrow();
    }

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        _recoverAppData();

        HttpRouter.AddRoute(IpcMethod.Get, "/install", async (request, _) =>
        {
            var searchParams = request.SafeUrl.SearchParams;
            var metadataUrl = request.QueryStringRequired("url");
            var jmmMetadata = await (await NativeFetchAsync(metadataUrl)).JsonAsync<JmmMetadata>();
            var url = new URL(metadataUrl);

            if (jmmMetadata is JmmMetadata metadata)
            {
                _openJmmMetadataInstallPage(metadata, url);
            }

            return jmmMetadata;
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/uninstall", async (request, _) =>
        {
            var searchParams = request.SafeUrl.SearchParams;
            var mmid = searchParams.ForceGet("mmid");
            var jmm = s_apps.GetValueOrDefault(mmid) ?? throw new Exception("");
            _openJmmMetadataUninstallPage(jmm);

            return true;
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/query", async (request, _) =>
        {
            return new AppQueryResult(
                s_apps.Values.Select(x => x.Metadata).ToList(), _installingApps.Values.ToList());
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/pause", async (_, ipc) =>
        {
            // TODO: 未实现JmmNMM暂停路由
            throw new NotImplementedException();
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/resume", async (_, ipc) =>
        {
            // TODO: 未实现JmmNMM恢复路由
            throw new NotImplementedException();
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/cancel", async (_, ipc) =>
        {
            // TODO: 未实现JmmNMM取消路由
            throw new NotImplementedException();
        });
    }

    // TODO: 打开下载页面待优化
    private async void _openJmmMetadataInstallPage(JmmMetadata jmmMetadata, URL url)
    {
        var vc = await IOSNativeMicroModule.RootViewController.WaitPromiseAsync();
        var data = NSData.FromString(jmmMetadata.ToJson(), NSStringEncoding.UTF8);

        if (!jmmMetadata.BundleUrl.StartsWith(Uri.UriSchemeHttp) && !jmmMetadata.BundleUrl.StartsWith(Uri.UriSchemeHttps))
        {
            jmmMetadata.BundleUrl = (new URL(new Uri(url.Uri, jmmMetadata.BundleUrl))).ToString();
        }

        await MainThread.InvokeOnMainThreadAsync(async () =>
        {
            try
            {
                var oldJmmMetadata = JmmMetadataDB.QueryJmmMetadata(jmmMetadata.Id);
                var initDownloadStatus = DownloadStatus.IDLE;

                if (oldJmmMetadata is not null)
                {
                    var oldSemver = new Semver(oldJmmMetadata.Version);
                    var newSemver = new Semver(jmmMetadata.Version);

                    if (newSemver.CompareTo(oldSemver) > 0)
                    {
                        initDownloadStatus = DownloadStatus.NewVersion;
                    }
                    else if (newSemver.CompareTo(oldSemver) == 0)
                    {
                        initDownloadStatus = DownloadStatus.Installed;
                    }
                }

                var manager = new DownloadAppManager(data, (nint)initDownloadStatus);
                
                manager.DownloadView.Frame = UIScreen.MainScreen.Bounds;
                JmmController.View.AddSubview(manager.DownloadView);
                vc.PushViewController(JmmController, true);

                await Task.Run(() =>
                {
                    // 点击下载
                    manager.ClickDownloadActionWithCallback(d =>
                    {
                        switch (d.ToString())
                        {
                            case "download":
                                if (initDownloadStatus == DownloadStatus.NewVersion)
                                {
                                    JmmController?.CloseApp(jmmMetadata.Id);
                                }
                                JmmDwebService.Add(new(
                                    jmmMetadata,
                                    manager.OnDownloadChangeWithDownloadStatus,
                                    manager.OnListenProgressWithProgress));

                                JmmDwebService.Start();
                                break;
                            case "open":
                                new JsMicroModule(jmmMetadata).Also((jsMicroModule) =>
                                {
                                    BootstrapContext.Dns.Install(jsMicroModule);
                                });
                                JmmController?.OpenApp(jmmMetadata.Id);
                                break;
                            default:
                                break;
                        }
                    });

                    // 点击返回
                    manager.OnBackActionWithCallback(() =>
                    {
                        vc.PopViewController(true);
                    });
                }).NoThrow();
            }
            catch (Exception e)
            {
                Console.Log("_openJmmMetadataInstallPage", e.Message);
                Console.Log("_openJmmMetadataInstallPage", e.StackTrace);
            }
        });
    }

    private void _openJmmMetadataUninstallPage(JsMicroModule jsMicroModule)
    {
        s_apps.Remove(jsMicroModule.Metadata.Id);
        BootstrapContext.Dns.UnInstall(jsMicroModule);
        JmmDwebService.UnInstall(jsMicroModule.Metadata.Id);
    }

    public record AppQueryResult(List<JmmMetadata> InstalledAppList, List<InstallingAppInfo> InstallingAppList);
    public record InstallingAppInfo(float Progress, JmmMetadata JmmMetadata);

    private Dictionary<Mmid, InstallingAppInfo> _installingApps = new();
}
