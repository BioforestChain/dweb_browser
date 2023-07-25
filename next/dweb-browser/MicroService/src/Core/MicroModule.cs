namespace DwebBrowser.MicroService.Core;

public abstract partial class MicroModule : Ipc.IMicroModuleInfo
{
    public Mmid Mmid { get; init; }
    public MicroModule(Mmid mmid)
    {
        Mmid = mmid;
    }
    public Router? Router = null;
    public abstract IpcSupportProtocols IpcSupportProtocols { get; init; }
    public abstract List<Dweb_DeepLink> Dweb_deeplinks { get; init; }

    private StatePromiseOut<MMState> _runningStateLock = StatePromiseOut<MMState>.StaticResolve(MMState.SHUTDOWN);
    public bool Running => _runningStateLock.Value == MMState.BOOTSTRAP;

    private async Task _beforeBootsStrap(IBootstrapContext bootstrapContext)
    {
        if (_runningStateLock.State == MMState.BOOTSTRAP)
        {
            throw new Exception(string.Format("module {0} already running", Mmid));
        }

        await _runningStateLock.WaitPromiseAsync(); // 确保已完成上一个状态
        _runningStateLock = new(MMState.BOOTSTRAP);

        _bootstrapContext = bootstrapContext;
    }

    private IBootstrapContext? _bootstrapContext = null;
    public IBootstrapContext BootstrapContext { get => _bootstrapContext ?? throw new Exception("module no run."); }

    protected abstract Task _bootstrapAsync(IBootstrapContext bootstrapContext);

    private void _afterBootstrap(IBootstrapContext dnsMM) => _runningStateLock.Resolve();

    public async Task BootstrapAsync(IBootstrapContext bootstrapContext)
    {
        await _beforeBootsStrap(bootstrapContext);
        try
        {
            await _bootstrapAsync(bootstrapContext);
        }
        finally
        {
            _afterBootstrap(bootstrapContext);
        }
    }

    private readonly HashSet<Signal> AfterShutdownSignal = new();
    public event Signal OnAfterShutdown
    {
        add { if(value != null) lock (AfterShutdownSignal) { AfterShutdownSignal.Add(value); } }
        remove { lock (AfterShutdownSignal) { AfterShutdownSignal.Remove(value); } }
    }

    protected async Task _beforeShutdownAsync()
    {
        if (_runningStateLock.State == MMState.SHUTDOWN)
        {
            throw new Exception(string.Format("module {0} already shutdown", Mmid));
        }

        await _runningStateLock.WaitPromiseAsync(); // 确保已经完成上一个状态
        _runningStateLock = new(MMState.SHUTDOWN);

        /// 关闭所有的通讯
        foreach (var ipc in _ipcSet)
        {
            await ipc.Close();
        }
        _ipcSet.Clear();
    }

    protected virtual async Task _shutdownAsync() { }

    protected async Task _afterShutdownAsync()
    {
        await AfterShutdownSignal.EmitAndClear();
        _runningStateLock.Resolve();
        _bootstrapContext = null;
    }

    public async Task ShutdownAsync()
    {
        await _beforeShutdownAsync();

        try
        {
            await _shutdownAsync();
        }
        finally
        {
            await _afterShutdownAsync();
        }
    }

    /**
     * <summary>
     * 连接池
     * </summary>
     */
    protected HashSet<Ipc> _ipcSet = new();
    protected void addToIpcSet(Ipc ipc)
    {
        this._ipcSet.Add(ipc);
        ipc.OnClose += async (_) =>
        {
            _ipcSet.Remove(ipc);
        };
    }

    /**
     * <summary>
     * 给内部程序自己使用的 onConnect，外部与内部建立连接时使用
     * 因为 NativeMicroModule 的内部程序在这里编写代码，所以这里会提供 onConnect 方法
     * 如果时 JsMicroModule 这个 onConnect 就是写在 WebWorker 那边了
     * </summary>
     */
    private readonly HashSet<Signal<Ipc, PureRequest>> ConnectSignal = new();
    public event Signal<Ipc, PureRequest> OnConnect
    {
        add { if(value != null) lock (ConnectSignal) { ConnectSignal.Add(value); } }
        remove { lock (ConnectSignal) { ConnectSignal.Remove(value); } }
    }

    /**
     * <summary>
     * 尝试连接到指定对象
     * </summary>
     */
    public async Task<Ipc?> ConnectAsync(Mmid mmid)
    {
        if (_bootstrapContext is not null)
        {
            var connectResult = await _bootstrapContext!.Dns.ConnectAsync(mmid);
            return connectResult.IpcForFromMM;
        }

        return null;
    }


    /**
     * <summary>
     * 收到一个连接，触发相关事件
     * </summary>
     */
    public Task BeConnectAsync(Ipc ipc, PureRequest reason)
    {
        this.addToIpcSet(ipc);
        ipc.OnEvent += async (ipcMessage, ipc, _) =>
        {
            if (ipcMessage.Name == "activity")
            {
                await _onActivityAsync(ipcMessage, ipc);
            }
        };

        return (ConnectSignal.Emit(ipc, reason)).ForAwait();
    }

    protected virtual async Task _onActivityAsync(IpcEvent Event, Ipc ipc) { }


}

internal class StatePromiseOut<T> : PromiseOut<T>
{
    public T State { get; init; }

    public StatePromiseOut(T state)
    {
        State = state;
    }

    public static new StatePromiseOut<T> StaticResolve(T state) => new StatePromiseOut<T>(state).Also(it => it.Resolve(state));

    public void Resolve() => base.Resolve(State);
}

internal enum MMState
{
    BOOTSTRAP,
    SHUTDOWN
}