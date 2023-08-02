
namespace DwebBrowser.MicroService.Sys.Boot;

public class BootNMM : NativeMicroModule
{
    static readonly Debugger Console = new("BootNMM");
    /**
     * 开机启动项注册表
     * TODO 这里需要从数据库中读取
     */
    private HashSet<Mmid> _registeredMmids = new();
    public override List<MicroModuleCategory> Categories { get; init; } = new()
    {
        MicroModuleCategory.Service,
        MicroModuleCategory.Hub_Service,
    };

    public override string? ShortName { get; set; } = "Boot";
    public BootNMM(List<Mmid>? initMmids = null) : base("boot.sys.dweb", "Boot Management")
    {
        if (initMmids is not null)
        {
            _registeredMmids.UnionWith(initMmids.ToHashSet());
        }
    }

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        HttpRouter.AddRoute(IpcMethod.Get, "/register", async (_, ipc) =>
        {
            return _register(ipc!.Remote.Mmid);
        });
        HttpRouter.AddRoute(IpcMethod.Get, "/unregister", async (_, ipc) =>
        {
            return _unregister(ipc!.Remote.Mmid);
        });

        /// 基于activity事件来启动开机项
        OnActivity += async (Event, ipc, _) =>
        {
            // 只响应 dns 模块的激活事件
            if (ipc.Remote.Mmid != "dns.std.dweb")
            {
                return;
            }

            foreach (var mmid in _registeredMmids)
            {
                Console.Log("OnActivity", "launch {0}", mmid);
                var fromIpc = await ConnectAsync(mmid);
                await (fromIpc?.PostMessageAsync(Event)).ForAwait();
            }
        };
    }

    protected override async Task _shutdownAsync()
    {
        Router!.Clear();
    }

    /**
     * <summary>
     * 注册一个boot程序
     * TODO 这里应该有用户授权，允许开机启动
     * <summary>
     */
    private bool _register(Mmid mmid) => _registeredMmids.Add(mmid);

    /**
     * <summary>
     * 移除一个boot程序
     * TODO 这里应该有用户授权，取消开机启动
     * </summary>
     */
    private bool _unregister(Mmid mmid) => _registeredMmids.Remove(mmid);
}

