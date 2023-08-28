namespace DwebBrowser.MicroService.Core;

public abstract partial class MicroModule : IMicroModule
{
    public Mmid Mmid { get; init; }
    public MicroModule(Mmid mmid)
    {
        Mmid = mmid;
    }
    public Router? Router = null;
    public abstract IpcSupportProtocols IpcSupportProtocols { get; init; }
    public abstract List<Dweb_DeepLink> Dweb_deeplinks { get; init; }
    public abstract List<MicroModuleCategory> Categories { get; init; }
    public abstract string Name { get; set; }
    public abstract TextDirectionType? Dir { get; set; }
    public abstract string? Version { get; set; }
    public abstract string? Lang { get; set; }
    public abstract string? ShortName { get; set; }
    public abstract string? Description { get; set; }
    public abstract List<ImageResource>? Icons { get; set; }
    public abstract List<ImageResource>? Screenshots { get; set; }
    public abstract DisplayModeType? Display { get; set; }
    public abstract OrientationType? Orientation { get; set; }
    public abstract string? ThemeColor { get; set; }
    public abstract string? BackgroundColor { get; set; }
    public abstract List<ShortcutItem>? Shortcuts { get; set; }

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
        add { if (value != null) lock (AfterShutdownSignal) { AfterShutdownSignal.Add(value); } }
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
        ActivitySignal.Clear();
        ConnectSignal.Clear();
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
    public void AddToIpcSet(Ipc ipc)
    {
        _ipcSet.Add(ipc);
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
        add { if (value != null) lock (ConnectSignal) { ConnectSignal.Add(value); } }
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
        AddToIpcSet(ipc);
        ipc.OnEvent += async (ipcMessage, ipc, _) =>
        {
            if (ipcMessage.Name == EIpcEvent.Activity.Event)
            {
                await ActivitySignal.Emit(ipcMessage, ipc);
            }
        };

        return (ConnectSignal.Emit(ipc, reason)).ForAwait();
    }

    private readonly HashSet<Signal<IpcEvent, Ipc>> ActivitySignal = new();
    public event Signal<IpcEvent, Ipc> OnActivity
    {
        add { if (value != null) lock (ActivitySignal) { ActivitySignal.Add(value); } }
        remove { lock (ActivitySignal) { ActivitySignal.Remove(value); } }
    }

    private readonly LazyBox<IMicroModuleManifest> Manifest = new();
    public virtual IMicroModuleManifest ToManifest() => Manifest.GetOrPut(() =>
        new MicroModuleManifest(
            Mmid,
            IpcSupportProtocols,
            Dweb_deeplinks,
            Categories,
            Name,
            Version,
            Dir,
            Lang,
            ShortName,
            Description,
            Icons,
            Screenshots,
            Display,
            Orientation,
            ThemeColor,
            BackgroundColor,
            Shortcuts));
}

internal class StatePromiseOut<T> : PromiseOut<T>
{
    public T State { get; init; }

    public StatePromiseOut(T state)
    {
        State = state;
    }

    public static new StatePromiseOut<T> StaticResolve(T state) => new StatePromiseOut<T>(state).Also(it => it.Resolve(state));

    public void Resolve() => Resolve(State);
}

internal enum MMState
{
    BOOTSTRAP,
    SHUTDOWN
}

public class MicroModuleManifest : IMicroModuleManifest, IEquatable<MicroModuleManifest>
{
    [Obsolete("使用带参数的构造函数", true)]
#pragma warning disable CS8618 // 在退出构造函数时，不可为 null 的字段必须包含非 null 值。请考虑声明为可以为 null。
    public MicroModuleManifest()
#pragma warning restore CS8618 // 在退出构造函数时，不可为 null 的字段必须包含非 null 值。请考虑声明为可以为 null。
    {
        /// 给JSON反序列化用的空参数构造函数
    }

    public MicroModuleManifest(
        Mmid mmid,
        IpcSupportProtocols ipcSupportProtocols,
        List<string> dweb_deeplinks,
        List<MicroModuleCategory> categories,
        string name,
        string? version = null,
        TextDirectionType? dir = null,
        string? lang = null,
        string? shortName = null,
        string? description = null,
        List<ImageResource>? icons = null,
        List<ImageResource>? screenshots = null,
        DisplayModeType? display = null,
        OrientationType? orientation = null,
        string? themeColor = null,
        string? backgroundColor = null,
        List<ShortcutItem>? shortcuts = null)
    {
        Mmid = mmid;
        IpcSupportProtocols = ipcSupportProtocols;
        Dweb_deeplinks = dweb_deeplinks;
        Categories = categories;
        Name = name;
        Version = version;
        Dir = dir;
        Lang = lang;
        ShortName = shortName;
        Description = description;
        Icons = icons;
        Screenshots = screenshots;
        Display = display;
        Orientation = orientation;
        ThemeColor = themeColor;
        BackgroundColor = backgroundColor;
        Shortcuts = shortcuts;
    }

    [JsonPropertyName("mmid")]
    public string Mmid { get; init; }
    [JsonPropertyName("ipc_support_protocols")]
    public IpcSupportProtocols IpcSupportProtocols { get; init; }
    [JsonPropertyName("dweb_deeplinks")]
    public List<Dweb_DeepLink> Dweb_deeplinks { get; init; }
    [JsonPropertyName("categories")]
    public List<MicroModuleCategory> Categories { get; init; }
    [JsonPropertyName("name")]
    public string Name { get; set; }
    [JsonPropertyName("version")]
    public string? Version { get; set; }
    [JsonPropertyName("dir")]
    public TextDirectionType? Dir { get; set; }
    [JsonPropertyName("lang")]
    public string? Lang { get; set; }
    [JsonPropertyName("short_name")]
    public string? ShortName { get; set; }
    [JsonPropertyName("description")]
    public string? Description { get; set; }
    [JsonPropertyName("icons")]
    public List<ImageResource>? Icons { get; set; }
    [JsonPropertyName("screenshots")]
    public List<ImageResource>? Screenshots { get; set; }
    [JsonPropertyName("display")]
    public DisplayModeType? Display { get; set; }
    [JsonPropertyName("orientation")]
    public OrientationType? Orientation { get; set; }
    [JsonPropertyName("theme_color")]
    public string? ThemeColor { get; set; }
    [JsonPropertyName("background_color")]
    public string? BackgroundColor { get; set; }
    [JsonPropertyName("shortcuts")]
    public List<ShortcutItem>? Shortcuts { get; set; }

    public virtual string ToJson() => JsonSerializer.Serialize(this);
    public static MicroModuleManifest? FromJson(string json) =>
        JsonSerializer.Deserialize<MicroModuleManifest>(json);

    public bool Equals(MicroModuleManifest? other)
    {
        return GetHashCode() == other?.GetHashCode();
    }

    public override int GetHashCode()
    {
        return Mmid.GetHashCode() ^
            IpcSupportProtocols.GetHashCode() ^
            Dweb_deeplinks.GetHashCode() ^
            Categories.GetHashCode() ^
            Name.GetHashCode() ^
            Version?.GetHashCode() ?? 0 ^
            Dir?.GetHashCode() ?? 0 ^
            Lang?.GetHashCode() ?? 0 ^
            ShortName?.GetHashCode() ?? 0 ^
            Description?.GetHashCode() ?? 0 ^
            Icons?.GetHashCode() ?? 0 ^
            Screenshots?.GetHashCode() ?? 0 ^
            Display?.GetHashCode() ?? 0 ^
            Orientation?.GetHashCode() ?? 0 ^
            ThemeColor?.GetHashCode() ?? 0 ^
            BackgroundColor?.GetHashCode() ?? 0 ^
            Shortcuts?.GetHashCode() ?? 0;
    }
}