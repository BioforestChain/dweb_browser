using System.Text.Json;
using Foundation;
using BrowserFramework;

namespace DwebBrowser.MicroService.Browser.Jmm;

public class JmmNMM : NativeMicroModule
{
    private static Dictionary<Mmid, JsMicroModule> s_apps = new();
    private static readonly List<JmmController> s_controllerList = new();
    public static Dictionary<Mmid, JsMicroModule> GetAndUpdateJmmNmmApps() => s_apps;

    public new List<Dweb_DeepLink> Dweb_deeplinks = new() { "dweb:install" };

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
        Task.Run(async () =>
        {
            //while (true)
            //{
            //    await Task.Delay(1000);

            //    try
            //    {
            //        await NativeFetchAsync(new URL("file://dns.sys.dweb/open").SearchParamsSet("app_id", "jmm.browser.dweb".EncodeURIComponent()).Uri);
            //        break;
            //    }
            //    catch
            //    { }
            //}

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
        });
    }

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        _recoverAppData();

        HttpRouter.AddRoute(IpcMethod.Get, "/install", async (request, _) =>
        {
            var searchParams = request.SafeUrl.SearchParams;
            var metadataUrl = request.QueryStringRequired("url");
            var jmmMetadata = await (await NativeFetchAsync(metadataUrl)).JsonAsync<JmmMetadata>()!;
            var url = new URL(metadataUrl);


            await _openJmmMetadataInstallPage(jmmMetadata, url);

            return jmmMetadata;
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/uninstall", async (request, _) =>
        {
            var searchParams = request.SafeUrl.SearchParams;
            var mmid = searchParams.ForceGet("mmid");
            var jmm = s_apps.GetValueOrDefault(mmid) ?? throw new Exception("");
            _openJmmMetadataUninstallPage(jmm.Metadata);

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

    private async Task _openJmmMetadataInstallPage(JmmMetadata jmmMetadata, URL url)
    {
        var vc = await IOSNativeMicroModule.RootViewController.WaitPromiseAsync();

        await MainThread.InvokeOnMainThreadAsync(async () =>
        {
            var data = new NSData(jmmMetadata.ToJson(), NSDataBase64DecodingOptions.None);
            var manager = new DownloadAppManager(data, false, false);
            JmmController.View.AddSubview(manager.DownloadView);
            vc.PushViewController(JmmController, true);
        });
    }

    private void _openJmmMetadataUninstallPage(JmmMetadata jmmMetadata)
    { }

    public record AppQueryResult(List<JmmMetadata> InstalledAppList, List<InstallingAppInfo> InstallingAppList);
    public record InstallingAppInfo(float Progress, JmmMetadata JmmMetadata);

    private Dictionary<Mmid, InstallingAppInfo> _installingApps = new();
}

