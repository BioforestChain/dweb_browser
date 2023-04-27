using DwebBrowser.MicroService;
using DwebBrowser.MicroService.Core;
using DwebBrowser.MicroService.Message;
using DwebBrowser.MicroService.Sys.Http.Net;
using DwebBrowser.Helper;


#nullable enable

namespace DwebBrowser.MicroService.Sys.Jmm;

public class JmmNMM : NativeMicroModule
{
    private static Dictionary<Mmid, JsMicroModule> s_apps = new();
    public static Dictionary<Mmid, JsMicroModule> GetAndUpdateJmmNmmApps() => s_apps;

    /// <summary>
    /// 获取当前App的数据配置
    /// </summary>
    /// <param name="mmid"></param>
    /// <returns></returns>
    public static JmmMetadata? GetBfsMetaData(Mmid mmid) => s_apps.GetValueOrDefault(mmid)?.Metadata;


    public JmmNMM() : base("jmm.sys.dweb")
    {
        // 启动的时候，从数据库中恢复 s_apps 对象
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

            JmmMetadataDB.QueryJmmMetadataList().Also(it =>
            {
                it.ForEach(entry =>
                {
                    s_apps.GetValueOrPut(entry.Key, () =>
                        new JsMicroModule(entry.Value).Also(jsMicroModule =>
                        {
                            try
                            {
                                BootstrapContext.Dns.Install(jsMicroModule);
                            }
                            catch { }
                        })
                    );
                });
            });
        });
    }

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        HttpRouter.AddRoute(IpcMethod.Get, "/install", async (request, _) =>
        {
            var metadataUrl = request.QueryValidate<string>("metadataUrl")!;
            var jmmMetadata = await (await NativeFetchAsync(metadataUrl)).Json<JmmMetadata>()!;
            _openJmmMetadataInstallPage(jmmMetadata);

            return jmmMetadata;
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/uninstall", async (request, _) =>
        {
            var mmid = request.QueryValidate<string>("mmid")!;
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

    private void _openJmmMetadataInstallPage(JmmMetadata jmmMetadata)
    { }

    private void _openJmmMetadataUninstallPage(JmmMetadata jmmMetadata)
    { }

    public record AppQueryResult(List<JmmMetadata> InstalledAppList, List<InstallingAppInfo> InstallingAppList);
    public record InstallingAppInfo(float Progress, JmmMetadata JmmMetadata);

    private Dictionary<Mmid, InstallingAppInfo> _installingApps = new();
}

