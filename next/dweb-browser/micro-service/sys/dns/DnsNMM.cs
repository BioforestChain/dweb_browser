using System.Net;
namespace micro_service.sys.dns;

public class DnsNMM : NativeMicroModule
{
    private Dictionary<Mmid, MicroModule> _installApps = new();
    private Dictionary<Mmid, MicroModule> _runningApps = new();

    public DnsNMM()
    {
        Mmid = "dns.sys.dweb";
    }

    public override string Mmid { get; init; }

    public async Task Bootstrap()
    {
        if (!Running)
        {

        }
    }

    public Task BootstrapMicroModule(MicroModule fromMM) =>
        fromMM.Bootstrap(new MyBootstrapContext(new MyDnsMicroModule(this, fromMM)));

    public class MyBootstrapContext : IBootstrapContext
    {
        public IDnsMicroModule Dns { get; init; }

        public MyBootstrapContext(MyDnsMicroModule dns)
        {
            Dns = dns;
        }
    }

    /** <summary>对等连接列表</summary> */
    private Dictionary<MicroModule, Dictionary<Mmid, PromiseOut<ConnectResult>>> _mmConnectsMap = new();

    /** <summary>为两个mm建立 ipc 通讯</summary> */
    private Task<ConnectResult> _connectTo(MicroModule fromMM, Mmid toMmid, HttpRequestMessage reason)
    {
        PromiseOut<ConnectResult> wait;
        lock (_mmConnectsMap)
        {
            /** 一个互联实例表 */
            var connectsMap = _mmConnectsMap.GetValueOrPut(fromMM, () =>
                new Dictionary<string, PromiseOut<ConnectResult>>());

            /**
             * 一个互联实例
             */
            wait = connectsMap.GetValueOrPut(toMmid, () =>
            {
                return new PromiseOut<ConnectResult>().Also(po =>
                {
                    Task.Run(() =>
                    {
                        var toMM = Open(toMmid);
                        Console.WriteLine($"DNS/connect {fromMM.Mmid} => {toMmid}");
                        var connects = NativeConnect.ConnectMicroModules(fromMM, toMM, reason);
                        po.Resolve(connects);
                        connects.IpcForFromMM.OnClose += ((_) =>
                        {
                            lock (_mmConnectsMap)
                            {
                                connectsMap.Remove(toMmid);
                            }

                            return null;
                        });
                    }).Background();
                });
            });
        }

        return wait.WaitPromiseAsync();
    }

    public class MyDnsMicroModule : IDnsMicroModule
    {
        private DnsNMM _dnsMM { get; init; }
        private MicroModule _fromMM { get; init; }

        public MyDnsMicroModule(DnsNMM dnsMM, MicroModule fromMM)
        {
            _dnsMM = dnsMM;
            _fromMM = fromMM;
        }

        public void Install(MicroModule mm)
        {
            throw new NotImplementedException();
        }

        public void UnInstall(MicroModule mm)
        {
            throw new NotImplementedException();
        }

        public Task<ConnectResult> Connect(string mmid, HttpRequestMessage? reason = null)
        {
            throw new NotImplementedException();
        }

        public Task Bootstrap(string mmid)
        {
            throw new NotImplementedException();
        }
    }

    protected override Task _bootstrap(IBootstrapContext bootstrapContext)
    {
        // install(this);
        _runningApps.Add(Mmid, this);

        /**
         * 对全局的自定义路由提供适配器
         * 对 nativeFetch 定义 file://xxx.dweb的解析
         */
        var cb = NativeFetch.NativeFetchAdaptersManager.Append((fromMM, request) =>
        {
            if (request.RequestUri is not null && request.RequestUri!.Scheme == "file"
                && request.RequestUri.Host.EndsWith(".dweb"))
            {
                var mmid = request.RequestUri.Host;
                Console.WriteLine($@"DNS/fetchAdapter
                        FromMM={fromMM.Mmid} >> requestMmid={mmid}: >> path={request.RequestUri.AbsolutePath}
                        >> {request.RequestUri}
                ");

                var microModule = _installApps.GetValueOrDefault(mmid);


                /// TODO: 异步返回lambada无法正确识别，待优化
                if (microModule is not null)
                {
                    var connectResult = _connectTo(fromMM, mmid, request).Result;
                    return connectResult.IpcForFromMM.Request(request).Result;
                }

                return new HttpResponseMessage(HttpStatusCode.BadGateway).Also(it =>
                    it.Content = new StringContent(request.RequestUri.ToString()));
            }

            return null;
        });
        _afterShutdownSignal += async (_) => { cb(); };

        return null;
    }

    protected override async Task _onActivity(IpcEvent Event, Ipc ipc)
    {
        /// 启动 boot 模块
        Open("boot.sys.dweb");
        var connectResult = await Connect("boot.sys.dweb");
        await connectResult.IpcForFromMM.PostMessageAsync(Event);
    }

    protected override async Task _shutdown()
    {
        foreach (var mm in _installApps)
        {
            await mm.Value.Shutdown();
        }

        _installApps.Clear();
    }

    /** <summary>安装应用</summary> */
    public void Install(MicroModule mm) => _installApps.Add(mm.Mmid, mm);

    /** <summary>卸载应用</summary> */
    public void UnInstall(MicroModule mm) => _installApps.Remove(mm.Mmid);

    /** <summary>查询应用</summary> */
    public MicroModule? Query(Mmid mmid) => _installApps.GetValueOrDefault(mmid);

    /** <summary>打开应用</summary> */
    public MicroModule Open(Mmid mmid) => _runningApps.GetValueOrPut(mmid, () =>
    {
        return Query(mmid)?.Also(it => BootstrapMicroModule(it)) ?? throw new Exception($"no found app {mmid}");
    });

    /** <summary>关闭应用</summary> */
    public async Task<int> Close(Mmid mmid)
    {
        var microModule = _runningApps.GetValueOrDefault(mmid);

        if (microModule is not null)
        {
            var _bool = _mmConnectsMap.GetValueOrDefault(microModule)?.Remove(mmid);
            await microModule.Shutdown();

            return _bool.GetValueOrDefault() ? 1 : 0;
        }

        return -1;
    }
}

