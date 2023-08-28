using System.Net;
using System.Web;
using DwebBrowser.MicroService.Http;

namespace DwebBrowser.MicroService.Browser.Jmm;

public class JmmNMM : NativeMicroModule
{
    static readonly Debugger Console = new("JmmNMM");

    static JmmNMM()
    {
        NativeFetch.NativeFetchAdaptersManager.Append(GetUsrFile);
    }

    public static async Task<PureResponse?> GetUsrFile(MicroModule remote, PureRequest request)
    {

        if (request.ParsedUrl is not null and var parsedUrl &&
            parsedUrl.Scheme == Uri.UriSchemeFile &&
            parsedUrl.FullHost is "" &&
            parsedUrl.Path.StartsWith("/usr/"))
        {
            var query = HttpUtility.ParseQueryString(parsedUrl.Query);
            var mode = query["mode"] ?? "auto";
            //var chunk = query["chunk"]?.ToIntOrNull() ?? 1024 * 1024;

            var relativePath = parsedUrl.Path;

            var baseDir = JsMicroModule.GetInstallPath(remote.Mmid, remote.Version);
            return await LocaleFile.ReadLocalFileAsResponse(baseDir, relativePath, mode, url: request.Url);
        }

        return null;

    }

    public override List<Dweb_DeepLink> Dweb_deeplinks { get; init; } = new() { "dweb:install" };
    public override List<MicroModuleCategory> Categories { get; init; } = new()
    {
        MicroModuleCategory.Service,
        MicroModuleCategory.Hub_Service,
    };

    public static JmmController JmmController = null!;

    public override string ShortName { get; set; } = "JMM";
    public JmmNMM() : base("jmm.browser.dweb", "Js MicroModule Management")
    {
    }

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        InstallJmmApps();

        HttpRouter.AddRoute(IpcMethod.Get, "/install", async (request, ipc) =>
        {
            var searchParams = request.SafeUrl.SearchParams;
            var metadataUrl = request.QueryStringRequired("url");
            var jmmMetadata = await (await NativeFetchAsync(metadataUrl)).JsonAsync<JmmAppInstallManifest>();
            var url = new URL(metadataUrl);

            if (jmmMetadata is JmmAppInstallManifest metadata)
            {
                await OpenJmmMetadataInstallPage(metadata, ipc, url);
            }

            return jmmMetadata;
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/uninstall", async (request, _) =>
        {
            var searchParams = request.SafeUrl.SearchParams;
            var mmid = searchParams.ForceGet("app_id");
            await OpenJmmMetadataUninstallPage(mmid);

            return new PureResponse(HttpStatusCode.OK, Body: new PureUtf8StringBody(@"{""ok"":true}"));
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/closeApp", async (request, _) =>
        {
            var mmid = request.QueryStringRequired("app_id");
            await JmmController.CloseApp(mmid);
            return true;
        });

        // App详情
        HttpRouter.AddRoute(IpcMethod.Get, "/detailApp", async (request, ipc) =>
        {
            var mmid = request.QueryStringRequired("app_id");
            var microModule = await bootstrapContext.Dns.Query(mmid);

            if (microModule is null)
            {
                return new PureResponse(HttpStatusCode.NotFound, Body: new PureUtf8StringBody("not found " + mmid));
            }

            if (microModule is JsMicroModule jsMicroModule)
            {
                var metadata = jsMicroModule.Metadata.Config;
                var jmmAppDownloadManifest = JmmAppDownloadManifest.FromInstallManifest(metadata);
                jmmAppDownloadManifest.DownloadStatus = GetCurrentDownloadStatus(metadata);

                await MainThread.InvokeOnMainThreadAsync(async () => await OpenJmmMetadataInstallPage(jmmAppDownloadManifest, ipc));

                return new PureResponse(HttpStatusCode.OK, Body: new PureUtf8StringBody(@"{""ok"":true}"));
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

    private DownloadStatus GetCurrentDownloadStatus(IJmmAppInstallManifest manifest)
    {
        var oldAmmMetadata = JmmDatabase.Instance.Find(manifest.Id);
        var initDownloadStatus = DownloadStatus.IDLE;

        if (oldAmmMetadata is not null)
        {
            var oldSemver = new Semver(oldAmmMetadata.Version);
            var newSemver = new Semver(manifest.Version);

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

    private async Task OpenJmmMetadataInstallPage(JmmAppInstallManifest manifest, Ipc ipc, URL? url = null)
    {
        if (!manifest.BundleUrl.StartsWith(Uri.UriSchemeHttp) && !manifest.BundleUrl.StartsWith(Uri.UriSchemeHttps))
        {
            if (url is not null)
            {
                manifest.BundleUrl = (new URL(new Uri(url.Uri, manifest.BundleUrl))).Href;
            }
        }

        try
        {
            await MainThread.InvokeOnMainThreadAsync(async () =>
            {
                var win = await WindowAdapterManager.Instance.CreateWindow(
                    new WindowState(ipc.Remote.Mmid, Mmid) { Mode = WindowMode.MAXIMIZE });

                JmmController = new JmmController(this, win);

                var jmmAppDownloadManifest = JmmAppDownloadManifest.FromInstallManifest(manifest);
                jmmAppDownloadManifest.DownloadStatus = GetCurrentDownloadStatus(manifest);
                await JmmController.OpenDownloadPageAsync(jmmAppDownloadManifest);
            });
        }
        catch (Exception e)
        {
            Console.Log("_openJmmMetadataInstallPage", e.Message);
            Console.Log("_openJmmMetadataInstallPage", e.StackTrace);
        }
    }

    private async Task OpenJmmMetadataUninstallPage(Mmid mmid)
    {
        BootstrapContext.Dns.UnInstall(mmid);
        JmmDwebService.UnInstall(mmid);
        JmmDatabase.Instance.Remove(mmid);
    }

    /// <summary>
    /// 注册所有已经下载的应用
    /// </summary>
    private void InstallJmmApps()
    {
        _ = Task.Run(() =>
        {
            foreach (var appInfo in JmmDatabase.Instance.All())
            {
                var metadata = new JsMMMetadata(appInfo);
                var jmm = new JsMicroModule(metadata);
                BootstrapContext.Dns.Install(jmm);
            }
        }).NoThrow();
    }
}
