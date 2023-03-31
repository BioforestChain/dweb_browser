
namespace micro_service.core;

public abstract class MicroModule : Ipc.MicroModuleInfo
{
    public abstract Mmid Mmid { get; init; }
    public Router? Router = null;

    private PromiseOut<bool> _runningStateLock = new PromiseOut<bool>();
    public bool Running { get => _runningStateLock.Value; }

    private async Task _beforeBootsStrap(IBootstrapContext bootstrapContext)
    {
        if (await _runningStateLock.WaitPromiseAsync())
        {
            throw new Exception($"module {Mmid} already running");
        }

        _runningStateLock = new PromiseOut<bool>();
        _bootstrapContext = bootstrapContext;
    }

    private IBootstrapContext? _bootstrapContext = null;
    public IBootstrapContext BootstrapContext { get => _bootstrapContext ?? throw new Exception("module no run."); }

    protected abstract Task _bootstrap(IBootstrapContext bootstrapContext);

    private void _afterBootstrap(IBootstrapContext dnsMM) => _runningStateLock.Resolve(true);

    public async Task Bootstrap(IBootstrapContext bootstrapContext)
    {
        await _beforeBootsStrap(bootstrapContext);
        try
        {
            await _bootstrap(bootstrapContext);
        }
        finally
        {
            _afterBootstrap(bootstrapContext);
        }
    }

    protected event Signal? _afterShutdownSignal;

    protected async Task _beforeShutdown()
    {
        if (!await _runningStateLock.WaitPromiseAsync())
        {
            throw new Exception($"module {Mmid} already shutdown");
        }

        _runningStateLock = new PromiseOut<bool>();

        /// 关闭所有的通讯

    }

    protected abstract Task _shutdown();

    protected async Task _afterShutdown()
    {
        await (_afterShutdownSignal?.Emit()).ForAwait();
        _afterShutdownSignal = null;
        _runningStateLock.Resolve(false);
        _bootstrapContext = null;
    }

    public async Task Shutdown()
    {
        await _beforeShutdown();

        try
        {
            await _shutdown();
        }
        finally
        {
            await _afterShutdown();
        }
    }

    /**
     * <summary>
     * 连接池
     * </summary>
     */
    protected HashSet<Ipc> _ipcSet = new();

    /**
     * <summary>
     * 给内部程序自己使用的 onConnect，外部与内部建立连接时使用
     * 因为 NativeMicroModule 的内部程序在这里编写代码，所以这里会提供 onConnect 方法
     * 如果时 JsMicroModule 这个 onConnect 就是写在 WebWorker 那边了
     * </summary>
     */
    protected event Signal<Ipc, HttpRequestMessage>? OnConnect;

    /**
     * <summary>
     * 尝试连接到指定对象
     * </summary>
     */
    public Task<ConnectResult> Connect(Mmid mmid, HttpRequestMessage? reason = null) =>
        _bootstrapContext!.Dns.Let(it =>
        {
            it.Bootstrap(mmid);
            return it.Connect(mmid);
        });

    /**
     * <summary>
     * 收到一个连接，触发相关事件
     * </summary>
     */
    public Task BeConnect(Ipc ipc, HttpRequestMessage reason)
    {
        _ipcSet.Add(ipc);
        ipc.OnClose += async (_) => _ipcSet.Remove(ipc);
        ipc.OnEvent += async (ipcMessage, ipc, _) =>
        {
            if (ipcMessage.Name == "activity")
            {
                await _onActivity(ipcMessage, ipc);
            }
        };

        return (OnConnect?.Emit(ipc, reason)).ForAwait();
    }

    protected abstract Task _onActivity(IpcEvent Event, Ipc ipc);
}

