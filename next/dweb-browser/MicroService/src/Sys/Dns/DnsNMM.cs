
using System.Collections.Generic;

namespace DwebBrowser.MicroService.Sys.Dns;

public class DnsNMM : NativeMicroModule
{
    // 已安装的应用
    private Dictionary<Mmid, MicroModule> _installApps = new();

    // 正在运行的应用
    private Dictionary<Mmid, PromiseOut<MicroModule>> _runningApps = new();

    public DnsNMM() : base("dns.sys.dweb")
    {
    }

    public async Task Bootstrap()
    {
        if (!Running)
        {
            await BootstrapMicroModule(this);
        }

        await OnActivity();
    }

    public Task BootstrapMicroModule(MicroModule fromMM) =>
        fromMM.BootstrapAsync(new MyBootstrapContext(new MyDnsMicroModule(this, fromMM)));

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
                    Task.Run(async () =>
                    {
                        Console.WriteLine(String.Format("DNS/opening {0} => {1}", fromMM.Mmid, toMmid));
                        var toMM = await Open(toMmid);
                        Console.WriteLine(String.Format("DNS/connect {0} => {1}", fromMM.Mmid, toMmid));
                        var connects = await NativeConnect.ConnectMicroModulesAsync(fromMM, toMM, reason);
                        po.Resolve(connects);
                        connects.IpcForFromMM.OnClose += async (_) =>
                        {
                            lock (_mmConnectsMap)
                            {
                                connectsMap.Remove(toMmid);
                            }
                        };
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
            _dnsMM.Install(mm);
        }

        public void UnInstall(MicroModule mm)
        {
            _dnsMM.UnInstall(mm);
        }

        public Task<ConnectResult> ConnectAsync(string mmid, HttpRequestMessage? reason = null)
        {
            return _dnsMM._connectTo(
                _fromMM, mmid, reason ?? new HttpRequestMessage(HttpMethod.Get, new Uri(String.Format("file://{0}", mmid))));
        }

        public Task BootstrapAsync(string mmid) => _dnsMM.Open(mmid);
    }

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        Install(this);
        _runningApps.Add(Mmid, PromiseOut<MicroModule>.StaticResolve(this));

        /**
         * 对全局的自定义路由提供适配器
         * 对 nativeFetch 定义 file://xxx.dweb的解析
         */
        var cb = NativeFetch.NativeFetchAdaptersManager.Append(async (fromMM, request) =>
        {
            if (request.RequestUri is not null && request.RequestUri!.Scheme == "file"
                && request.RequestUri.Host.EndsWith(".dweb"))
            {
                var mmid = request.RequestUri.Host;

                var microModule = _installApps.GetValueOrDefault(mmid);

                if (microModule is not null)
                {
                    var connectResult = await _connectTo(fromMM, mmid, request);
                    Console.WriteLine(String.Format("DNS/request/{0} => {1} [{2}] {3}", fromMM.Mmid, mmid, request.Method.Method, request.RequestUri.AbsolutePath));
                    return await connectResult.IpcForFromMM.Request(request);
                }

                return new HttpResponseMessage(HttpStatusCode.BadGateway).Also(it =>
                    it.Content = new StringContent(request.RequestUri.ToString()));
            }

            return null;
        });
        _onAfterShutdown += async (_) => { cb(); };

        // 打开应用
        HttpRouter.AddRoute(HttpMethod.Get.Method, "/open", async (request, _) =>
        {
            Console.WriteLine(String.Format("open/{0} {1}", Mmid, request.RequestUri?.AbsolutePath));
            await Open(request.QueryValidate<Mmid>("app_id")!);
            return true;
        });

        // 关闭应用
        // TODO 能否关闭一个应该应该由应用自己决定
        HttpRouter.AddRoute(HttpMethod.Get.Method, "/close", async (request, _) =>
        {
            Console.WriteLine(String.Format("close/{0} {1}", Mmid, request.RequestUri?.AbsolutePath));
            await Open(request.QueryValidate<string>("app_id")!);
            return true;
        });
    }

    protected override Task _onActivityAsync(IpcEvent Event, Ipc ipc) => OnActivity(Event);

    public async Task OnActivity(IpcEvent? Event = null)
    {
        if (Event is null)
        {
            Event = IpcEvent.FromUtf8("activity", "");
        }

        /// 启动 boot 模块
        await Open("boot.sys.dweb");
        var connectResult = await ConnectAsync("boot.sys.dweb");
        await connectResult.IpcForFromMM.PostMessageAsync(Event);
    }

    protected override async Task _shutdownAsync()
    {
        foreach (var mm in _installApps)
        {
            await mm.Value.ShutdownAsync();
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
    private PromiseOut<MicroModule> _Open(Mmid mmid)
    {
        if (!_runningApps.TryGetValue(mmid, out var po))
        {
            _runningApps[mmid] = po = new();
            Task.Run(async () =>
            {
                var app = Query(mmid);
                if (app is not null)
                {
                    await BootstrapMicroModule(app);
                    po.Resolve(app);
                }
                else
                {
                    po.Reject(String.Format("no found app: {0}", mmid));
                }
            });
        }
        return po;
    }
    /** <summary>打开应用</summary> */
    public Task<MicroModule> Open(Mmid mmid) => _Open(mmid).WaitPromiseAsync();

    /** <summary>关闭应用</summary> */
    public async Task<int> Close(Mmid mmid)
    {
        if (_runningApps.Remove(mmid, out var microModulePo))
        {
            var microModule = await microModulePo.WaitPromiseAsync();
            var _bool = _mmConnectsMap.GetValueOrDefault(microModule)?.Remove(mmid);
            await microModule.ShutdownAsync();

            return _bool.GetValueOrDefault() ? 1 : 0;
        }

        return -1;
    }
}

