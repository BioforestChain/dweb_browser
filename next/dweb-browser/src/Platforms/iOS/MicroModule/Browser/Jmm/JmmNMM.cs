using System.Net;
using System.Web;
using BrowserFramework;
using DwebBrowser.MicroService.Http;
using Foundation;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Jmm;

public class JmmNMM : NativeMicroModule
{
    static Debugger Console = new("JmmNMM");

    private static readonly List<JmmController> s_controllerList = new();
    public static Dictionary<Mmid, JsMicroModule> JmmApps = new();

    static JmmNMM()
    {
        NativeFetch.NativeFetchAdaptersManager.Append(GetUsrFile);
    }

    public static async Task<PureResponse?> GetUsrFile(MicroModule remote, PureRequest request)
    {

        if (request.ParsedUrl is not null and var parsedUrl && parsedUrl.Scheme == Uri.UriSchemeFile && parsedUrl.FullHost is "" && parsedUrl.Path.StartsWith("/usr/"))
        {
            var query = HttpUtility.ParseQueryString(parsedUrl.Query);
            var mode = query["mode"] ?? "auto";
            var chunk = query["chunk"]?.ToIntOrNull() ?? 1024 * 1024;

            var relativePath = string.Empty;
            var baseDir = string.Empty;

            if (JmmApps.TryGetValue(remote.Mmid, out var jsMicroModule))
            {
                relativePath = parsedUrl.Path;
                baseDir = JsMicroModule.GetInstallPath(jsMicroModule.Metadata);
                return await LocaleFile.ReadLocalFileAsResponse(baseDir, relativePath, mode, url: request.Url);
            }
            return new PureResponse(HttpStatusCode.InternalServerError, Url: request.Url);
        }

        return null;

    }

    public override List<Dweb_DeepLink> Dweb_deeplinks { get; init; } = new() { "dweb:install" };

    /// <summary>
    /// 获取当前App的数据配置
    /// </summary>
    /// <param name="mmid"></param>
    /// <returns></returns>
    public static JmmMetadata? GetBfsMetaData(Mmid mmid) => JmmApps.GetValueOrDefault(mmid)?.Metadata;

    public static JmmController JmmController
    {
        get => s_controllerList.FirstOrDefault();
    }

    public JmmNMM() : base("jmm.browser.dweb")
    {
        s_controllerList.Add(new(this));

        JmmMetadataDB.JmmMetadataUpdate.OnChange += async (value, oldValue, _) =>
        {
            if (value > oldValue)
            {
                _recoverAppData();
            }
        };
    }

    private void _recoverAppData()
    {
        // 启动的时候，从数据库中恢复 s_apps 对象
        _ = Task.Run(async () =>
        {
            lock (JmmApps)
            {
                JmmApps.Clear();
                foreach (var entry in JmmMetadataDB.GetJmmMetadataEnumerator())
                {
                    JmmApps.GetValueOrPut(entry.Key, () =>
                        new JsMicroModule(entry.Value).Also(jsMicroModule =>
                        {
                            try
                            {
                                BootstrapContext.Dns.Install(jsMicroModule);
                            }
                            catch { }
                        }));
                }
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
            var jmm = JmmApps.GetValueOrDefault(mmid) ?? throw new Exception("");
            _openJmmMetadataUninstallPage(jmm);

            return true;
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/query", async (request, _) =>
        {
            return new AppQueryResult(
                JmmApps.Values.Select(x => x.Metadata).ToList(), _installingApps.Values.ToList());
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/pause", async (_, ipc) =>
        {
            return JmmDwebService.UpdateDownloadControlStatus(ipc.Remote.Mmid, DownloadControlStatus.Pause);
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/resume", async (_, ipc) =>
        {
            return JmmDwebService.UpdateDownloadControlStatus(ipc.Remote.Mmid, DownloadControlStatus.Resume);
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/cancel", async (_, ipc) =>
        {
            return JmmDwebService.UpdateDownloadControlStatus(ipc.Remote.Mmid, DownloadControlStatus.Cancel);
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

                // 无法push同一个UIViewController的实例两次
                var index = vc.ViewControllers?.ToList().FindIndex(uvc => uvc == JmmController);
                if (index >= 0)
                {
                    // 不是当前controller时，推到最新
                    if (index != vc.ViewControllers!.Length - 1)
                    {
                        vc.PopToViewController(JmmController, true);
                    }
                }
                else
                {
                    vc.PushViewController(JmmController, true);
                }

                // 点击下载
                manager.ClickDownloadActionWithCallback(async d =>
                {
                    switch (d.ToString())
                    {
                        case "download":
                            if (initDownloadStatus == DownloadStatus.NewVersion)
                            {
                                JmmController?.CloseApp(jmmMetadata.Id);
                            }
                            var jmmDownload = JmmDwebService.Add(jmmMetadata,
                                 manager.OnDownloadChangeWithDownloadStatus,
                                 manager.OnListenProgressWithProgress);

                            JmmDwebService.Start();
                            break;
                        case "open":
                            Console.Log("open", jmmMetadata.ToJson());
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
            }
            catch (Exception e)
            {
                Console.Log("_openJmmMetadataInstallPage", e.Message);
                Console.Log("_openJmmMetadataInstallPage", e.StackTrace);
            }
        });
    }

    private async void _openJmmMetadataUninstallPage(JsMicroModule jsMicroModule)
    {
        var mmid = jsMicroModule.Metadata.Id;
        JmmApps.Remove(mmid);
        BootstrapContext.Dns.UnInstall(jsMicroModule);
        JmmDwebService.UnInstall(jsMicroModule.Metadata);
        JmmMetadataDB.RemoveJmmMetadata(mmid);
    }

    public record AppQueryResult(List<JmmMetadata> InstalledAppList, List<InstallingAppInfo> InstallingAppList);
    public record InstallingAppInfo(float Progress, JmmMetadata JmmMetadata);

    private Dictionary<Mmid, InstallingAppInfo> _installingApps = new();
}
