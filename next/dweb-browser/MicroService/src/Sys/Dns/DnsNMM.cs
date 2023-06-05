using DwebBrowser.MicroService.Sys.Http;

namespace DwebBrowser.MicroService.Sys.Dns;

public class DnsNMM : NativeMicroModule
{
    static readonly Debugger Console = new("DnsNMM");
    // 已安装的应用
    private Dictionary<Mmid, MicroModule> _installApps = new();

    // 正在运行的应用
    private Dictionary<Mmid, PromiseOut<MicroModule>> _runningApps = new();
    public new List<string> Dweb_deeplinks = new() { "dweb:open" };

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

    record MM(Mmid fromMmid, Mmid toMmid)
    {
        public static Dictionary<Mmid, Dictionary<Mmid, MM>> Values = new();
        public static MM From(Mmid fromMmid, Mmid toMmid) =>
            Values.GetValueOrPut(fromMmid, () => new()).GetValueOrPut(toMmid, () => new MM(fromMmid, toMmid));
    }

    /** <summary>对等连接列表</summary> */
    private Dictionary<MM, PromiseOut<ConnectResult>> _mmConnectsMap = new();

    /** <summary>为两个mm建立 ipc 通讯</summary> */
    private Task<ConnectResult> _connectTo(MicroModule fromMM, Mmid toMmid, PureRequest reason)
    {
        PromiseOut<ConnectResult> wait;
        lock (_mmConnectsMap)
        {
            var mmKey = MM.From(fromMM.Mmid, toMmid);

            /**
             * 一个互联实例
             */
            wait = _mmConnectsMap.GetValueOrPut(mmKey, () =>
            {
                return new PromiseOut<ConnectResult>().Also(async po =>
                {
                    Console.Log("ConnectTo", "DNS/opening {0} => {1}", fromMM.Mmid, toMmid);
                    var toMM = await Open(toMmid);
                    Console.Log("ConnectTo", "DNS/connect {0} => {1}", fromMM.Mmid, toMmid);
                    var connectResult = await NativeConnect.ConnectMicroModulesAsync(fromMM, toMM, reason);
                    connectResult.IpcForFromMM.OnClose += async (_) =>
                    {
                        lock (_mmConnectsMap)
                        {
                            _mmConnectsMap.Remove(mmKey);
                        }
                    };
                    po.Resolve(connectResult);

                    /// 如果可以，反向存储
                    if (connectResult.IpcForToMM is not null)
                    {
                        var mmkey2 = MM.From(toMmid, fromMM.Mmid);
                        lock (_mmConnectsMap)
                        {
                            _mmConnectsMap.GetValueOrPut(mmkey2, () =>
                            {
                                return new PromiseOut<ConnectResult>().Also(po2 =>
                                {
                                    var connectResult2 = new ConnectResult(
                                    connectResult.IpcForToMM, connectResult.IpcForFromMM);

                                    connectResult2.IpcForFromMM.OnClose += async (_) =>
                                    {
                                        lock (_mmConnectsMap)
                                        {
                                            _mmConnectsMap.Remove(mmkey2);
                                        }
                                    };
                                    po2.Resolve(connectResult2);
                                });
                            });
                        }
                    }
                });
            });

            Console.Log("_connectTo", "new MM => {0}", _mmConnectsMap.GetValueOrDefault(new MM(fromMM.Mmid, toMmid)));
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

        public MicroModule? Query(Mmid mmid) =>
            _dnsMM.Query(mmid) is MicroModule mm ? mm : null;

        public void Restart(Mmid mmid)
        {
            Task.Run(async () =>
            {
                // 关闭后端连接
                await _dnsMM.Close(mmid);
                // TODO 防止启动过快出现闪屏
                await Task.Delay(200);
                await _dnsMM.Open(mmid);
            });
        }

        public Task<ConnectResult> ConnectAsync(string mmid, PureRequest? reason = null)
        {
            return _dnsMM._connectTo(
                _fromMM, mmid, reason ?? new PureRequest(string.Format("file://{0}", mmid), IpcMethod.Get));
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
            if (request.ParsedUrl is not null and var parsedUrl && parsedUrl.Scheme is "file" && parsedUrl.Hostname is var mmid && mmid.EndsWith(".dweb"))
            {
                var microModule = _installApps.GetValueOrDefault(mmid);

                if (microModule is not null)
                {
                    var connectResult = await _connectTo(fromMM, mmid, request);
                    Console.Log("NativeFetch", "DNS/request/{0} => {1} [{2}] {3}", fromMM.Mmid, mmid, request.Method.Method, parsedUrl.Path);
                    return await connectResult.IpcForFromMM.Request(request);
                }

                return new PureResponse(HttpStatusCode.BadGateway, Body: new PureUtf8StringBody(request.Url));
            }

            return null;
        });
        var deeplinkCb = NativeFetch.NativeFetchAdaptersManager.Append(async (fromMM, request) =>
        {
            if (request.ParsedUrl is not null and var parsedUrl && parsedUrl.Scheme is "dweb")
            {
                //var connectResult = await _connectTo(fromMM, mmid, request);
                //Console.Log("NativeFetch", "DNS/request/{0} => {1} [{2}] {3}", fromMM.Mmid, mmid, request.Method.Method, parsedUrl.Path);
                //return await connectResult.IpcForFromMM.Request(request);
                foreach (var app in _installApps.Values)
                {
                    app.Dweb_deeplinks.Find(dl => parsedUrl.ToString().StartsWith(dl));
                }
            }

            return null;
        });
        
        _onAfterShutdown += async (_) => { cb(); };

        var Query_appId = (URL parsedUrl) => parsedUrl.SearchParams.Get("app_id") ?? throw new ArgumentException("no found app_id");

        // 打开应用
        HttpRouter.AddRoute(IpcMethod.Get, "/open", async (request, _) =>
        {
            var parsedUrl = request.SafeUrl;
            Console.Log("Open", "{0} {1}", Mmid, parsedUrl.Path);
            await Open(parsedUrl.SearchParams.ForceGet("app_id"));
            return true;
        });

        // 关闭应用
        // TODO 能否关闭一个应该应该由应用自己决定
        HttpRouter.AddRoute(IpcMethod.Get, "/close", async (request, _) =>
        {
            var parsedUrl = request.SafeUrl;
            Console.Log("Close", "{0} {1}", Mmid, parsedUrl.Path);
            await Close(parsedUrl.SearchParams.ForceGet("app_id"));
            return true;
        });

        // deeplink
        HttpRouter.AddRoute(new Gateway.RouteConfig("open/", IpcMethod.Get), async (request, _) =>
        {
            var parsedUrl = request.SafeUrl;
            var app_id = parsedUrl.Path.Replace("open/", "");
            await Open(app_id);
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
                try
                    {

                    var app = Query(mmid);
                    if (app is not null)
                    {
                        await BootstrapMicroModule(app);
                        po.Resolve(app);
                    }
                    else
                    {
                        po.Reject(string.Format("no found app: {0}", mmid));
                        }
                    }
                catch (Exception e) {
                    Console.Error("Open", "{0}", e);
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
            //var _bool1 = _mmConnectsMap.Remove(MM.From(microModule.Mmid, "js.browser.dweb"));
            //var _bool2 = _mmConnectsMap.Remove(MM.From("js.browser.dweb", microModule.Mmid));
            await microModule.ShutdownAsync();

            //return _bool1 && _bool2 ? 1 : 0;
            return 1;
        }

        return -1;
    }
}

