using System.Net;
using System.Web;
using DwebBrowser.MicroService.Http;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Jmm;

public class JmmNMM : NativeMicroModule
{
    static readonly Debugger Console = new("JmmNMM");

    private static readonly List<JmmController> s_controllerList = new();
    public static readonly Dictionary<Mmid, JsMicroModule> JmmApps = new();

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
            //var chunk = query["chunk"]?.ToIntOrNull() ?? 1024 * 1024;

            if (JmmApps.TryGetValue(remote.Mmid, out var jsMicroModule))
            {
                var relativePath = parsedUrl.Path;
                var baseDir = JsMicroModule.GetInstallPath(jsMicroModule.Metadata);
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
    public static AppMetaData? GetBfsMetaData(Mmid mmid) => JmmApps.GetValueOrDefault(mmid)?.Metadata;

    public static JmmController JmmController
    {
        get => s_controllerList.FirstOrDefault();
    }

    public JmmNMM() : base("jmm.browser.dweb")
    {
        s_controllerList.Add(new(this));
    }

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        //_recoverAppData();

        HttpRouter.AddRoute(IpcMethod.Get, "/install", async (request, _) =>
        {
            var searchParams = request.SafeUrl.SearchParams;
            var metadataUrl = request.QueryStringRequired("url");
            var jmmMetadata = await (await NativeFetchAsync(metadataUrl)).JsonAsync<AppMetaData>();
            var url = new URL(metadataUrl);

            if (jmmMetadata is AppMetaData metadata)
            {
                _openJmmMetadataInstallPage(metadata, url);
            }

            return jmmMetadata;
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/uninstall", async (request, _) =>
        {
            var searchParams = request.SafeUrl.SearchParams;
            var mmid = searchParams.ForceGet("app_id");
            var jmm = JmmApps.GetValueOrDefault(mmid) ?? throw new Exception("");
            _openJmmMetadataUninstallPage(jmm);

            return true;
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/query", async (request, _) =>
        {
            return new AppQueryResult(
                JmmApps.Values.Select(x => x.Metadata).ToList(), _installingApps.Values.ToList());
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/openApp", async (request, _) =>
        {
            var mmid = request.QueryStringRequired("app_id");
            return await bootstrapContext.Dns.Open(mmid);
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/closeApp", async (request, _) =>
        {
            var mmid = request.QueryStringRequired("app_id");
            return await bootstrapContext.Dns.Close(mmid);
        });

        // App详情
        HttpRouter.AddRoute(IpcMethod.Get, "/detailApp", async (request, ipc) =>
        {
            var mmid = request.QueryStringRequired("app_id");
            var jsMicroModule = JmmApps.GetValueOrDefault(mmid);

            if (jsMicroModule is not null)
            {
                var initDownloadStatus = DownloadStatus.Installed;

                await JmmController.OpenDownloadPageAsync(jsMicroModule.Metadata, initDownloadStatus);

                return true;
            }

            return new PureResponse(HttpStatusCode.NotFound, Body: new PureUtf8StringBody("not found " + mmid));
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

    private DownloadStatus _getCurrentDownloadStatus(AppMetaData appMetadata)
    {
        var oldAmmMetadata = JmmDatabase.Instance.Find(appMetadata.Id);
        var initDownloadStatus = DownloadStatus.IDLE;

        if (oldAmmMetadata is not null)
        {
            var oldSemver = new Semver(oldAmmMetadata.Version);
            var newSemver = new Semver(appMetadata.Version);

            if (newSemver.CompareTo(oldSemver) > 0)
            {
                initDownloadStatus = DownloadStatus.NewVersion;
            }
            else if (newSemver.CompareTo(oldSemver) == 0)
            {
                initDownloadStatus = DownloadStatus.Installed;
            }
        }

        return initDownloadStatus;
    }

    private async void _openJmmMetadataInstallPage(AppMetaData appMetadata, URL url)
    {
        if (!appMetadata.BundleUrl.StartsWith(Uri.UriSchemeHttp) && !appMetadata.BundleUrl.StartsWith(Uri.UriSchemeHttps))
        {
            appMetadata.BundleUrl = (new URL(new Uri(url.Uri, appMetadata.BundleUrl))).Href;
        }

        try
        {
            var initDownloadStatus = _getCurrentDownloadStatus(appMetadata);
            await JmmController.OpenDownloadPageAsync(appMetadata, initDownloadStatus);
        }
        catch (Exception e)
        {
            Console.Log("_openJmmMetadataInstallPage", e.Message);
            Console.Log("_openJmmMetadataInstallPage", e.StackTrace);
        }
    }

    private async void _openJmmMetadataUninstallPage(JsMicroModule jsMicroModule)
    {
        var mmid = jsMicroModule.Metadata.Id;
        JmmApps.Remove(mmid);
        BootstrapContext.Dns.UnInstall(jsMicroModule);
        JmmDwebService.UnInstall(jsMicroModule.Metadata);
        JmmDatabase.Instance.Remove(mmid);
    }

    public record AppQueryResult(List<AppMetaData> InstalledAppList, List<InstallingAppInfo> InstallingAppList);
    public record InstallingAppInfo(float Progress, AppMetaData JmmMetadata);

    private readonly Dictionary<Mmid, InstallingAppInfo> _installingApps = new();
}
