using System;

namespace DwebBrowser.MicroService.Sys.Boot;

public class BootNMM: NativeMicroModule
{
    /**
     * 开机启动项注册表
     * TODO 这里需要从数据库中读取
     */
    private HashSet<Mmid> _registeredMmids = new();

    public BootNMM(List<Mmid>? initMmids = null)
	{
        Mmid = "boot.sys.dweb";

        if (initMmids is not null)
        {
            _registeredMmids.UnionWith(initMmids.ToHashSet());
        }

        Router = new Dictionary<string, Func<Dictionary<string, string>, object>>();
	}

    public override string Mmid { get; init; }

    protected override Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        throw new NotImplementedException();
    }

    protected override async Task _onActivityAsync(IpcEvent Event, Ipc ipc)
    {
        foreach (var mmid in _registeredMmids)
        {
            Console.WriteLine($"launch {mmid}");
            await BootstrapContext.Dns.BootstrapAsync(mmid);
            var connectResult = await BootstrapContext.Dns.ConnectAsync(mmid);
            await connectResult.IpcForFromMM.PostMessageAsync(Event);
        }
    }

    protected override Task _shutdownAsync()
    {
        return Task.Run(() => Router!.Clear());
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

