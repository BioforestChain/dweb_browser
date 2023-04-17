using DwebBrowser.MicroService;
using DwebBrowser.MicroService.Core;
using DwebBrowser.MicroService.Message;
using DwebBrowser.MicroService.Sys.Http.Net;
using DwebBrowser.Helper;
using System.Linq;

#nullable enable

namespace DwebBrowser.WebModule.Jmm;

public class JmmNMM : NativeMicroModule
{
    public override string Mmid { get; init; }

    private static Dictionary<Mmid, JsMicroModule> s_apps = new();
    public static Dictionary<Mmid, JsMicroModule> GetAndUpdateJmmNmmApps() => s_apps;

    /// <summary>
    /// 获取当前App的数据配置
    /// </summary>
    /// <param name="mmid"></param>
    /// <returns></returns>
    public static JmmMetadata? GetBfsMetaData(Mmid mmid) => s_apps.GetValueOrDefault(mmid)?.Metadata;


    public JmmNMM()
    {
        Mmid = "jmm.sys.dweb";

        Task.Run(async () =>
        {
            while (true)
            {
                await Task.Delay(1000);

                try
                {
                    await NativeFetchAsync(new Uri("file://dns.sys.dweb/open").AppendQuery("app_id", "jmm.sys.dweb".EncodeURIComponent()));
                    break;
                }
                catch
                { }
            }

            // TODO: JmmMetadataDB功能未实现
        });
    }

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        HttpRouter.AddRoute(HttpMethod.Get.Method, "/install", async (request, _) =>
        {
            var metadataUrl = request.QueryValidate<string>("metadataUrl")!;
            var jmmMetadata = await (await NativeFetchAsync(metadataUrl)).Json<JmmMetadata>()!;
            _openJmmMetadataInstallPage(jmmMetadata);

            return jmmMetadata;
        });

        HttpRouter.AddRoute(HttpMethod.Get.Method, "/uninstall", async (request, _) =>
        {
            var mmid = request.QueryValidate<string>("mmid")!;
            var jmm = s_apps.GetValueOrDefault(mmid) ?? throw new Exception("");
            _openJmmMetadataUninstallPage(jmm.Metadata);

            return true;
        });

        HttpRouter.AddRoute(HttpMethod.Get.Method, "/query", async (request, _) =>
        {
            return new AppQueryResult(
                s_apps.Values.Select(x => x.Metadata).ToList(), _installingApps.Values.ToList());
        });

        HttpRouter.AddRoute(HttpMethod.Get.Method, "/pause", async (_, ipc) =>
        {
            // TODO: 未实现JmmNMM暂停路由
            throw new NotImplementedException();
        });

        HttpRouter.AddRoute(HttpMethod.Get.Method, "/resume", async (_, ipc) =>
        {
            // TODO: 未实现JmmNMM恢复路由
            throw new NotImplementedException();
        });

        HttpRouter.AddRoute(HttpMethod.Get.Method, "/cancel", async (_, ipc) =>
        {
            // TODO: 未实现JmmNMM取消路由
            throw new NotImplementedException();
        });
    }

    private void _openJmmMetadataInstallPage(JmmMetadata jmmMetadata)
    { }

    private void _openJmmMetadataUninstallPage(JmmMetadata jmmMetadata)
    { }

    public record AppQueryResult(List<JmmMetadata> InstalledAppList, List<InstallingAppInfo> InstallingAppList);
    public record InstallingAppInfo(float Progress, JmmMetadata JmmMetadata);

    private Dictionary<Mmid, InstallingAppInfo> _installingApps = new();

    protected override async Task _onActivityAsync(IpcEvent Event, Ipc ipc)
    {
        
    }

    protected override async Task _shutdownAsync()
    {
        
    }
}

