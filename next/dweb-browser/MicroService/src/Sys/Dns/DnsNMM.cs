using DwebBrowser.MicroService.Sys.Http;

namespace DwebBrowser.MicroService.Sys.Dns;

public class DnsNMM : NativeMicroModule
{
    static readonly Debugger Console = new("DnsNMM");
    // 已安装的应用
    private Dictionary<Mmid, MicroModule> _installApps = new();

    // 正在运行的应用
    private Dictionary<Mmid, PromiseOut<MicroModule>> _runningApps = new();
    public override List<Dweb_DeepLink> Dweb_deeplinks { get; init; } = new() { "dweb:open" };
    public override List<MicroModuleCategory> Categories { get; init; } = new()
    {
        MicroModuleCategory.Service,
        MicroModuleCategory.Routing_Service,
    };

    public new const string Name = "Dweb Name System";
    public override string? ShortName { get; set; } = "DNS";
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
        fromMM.BootstrapAsync(new MyBootstrapContext(new MyDnsMicroModule(this, fromMM))).NoThrow();

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

        public bool UnInstall(MicroModule mm)
        {
            return _dnsMM.UnInstall(mm);
        }

        public async Task<IMicroModuleManifest?> Query(Mmid mmid) =>
            (await _dnsMM.Query(mmid)) is MicroModule mm ? mm : null;

        public async Task Restart(Mmid mmid)
        {
            // 关闭后端连接
            await _dnsMM.Close(mmid);
            await _dnsMM.Open(mmid);
        }

        public Task<ConnectResult> ConnectAsync(Mmid mmid, PureRequest? reason = null)
        {
            return _dnsMM._connectTo(
                _fromMM, mmid, reason ?? new PureRequest(string.Format("file://{0}", mmid), IpcMethod.Get));
        }

        public async Task<bool> Open(Mmid mmid)
        {
            /// 已经在运行中了，直接返回true
            if (_dnsMM._runningApps.ContainsKey(mmid))
            {
                return true;
            }

            /// 尝试运行，成功就返回true
            try
            {
                await _dnsMM.Open(mmid);
                return true;
            }
            catch
            {
                return false;
            }
        }

        public async Task<bool> Close(Mmid mmid)
        {
            if (_dnsMM._runningApps.ContainsKey(mmid))
            {
                if (await _dnsMM.Close(mmid) == 1)
                {
                    return true;
                }
            }

            return false;
        }

        public async Task<MicroModule[]> Search(MicroModuleCategory category)
        {
            return _dnsMM.Search(category).ToArray();
        }
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

        /// dweb-deeplinks
        var deeplinkCb = NativeFetch.NativeFetchAdaptersManager.Append(async (fromMM, request) =>
        {
            if (request.ParsedUrl is not null and var parsedUrl && parsedUrl.Scheme is "dweb" && parsedUrl.Hostname == "")
            {
                Console.Log("NativeFetch", "DNS/dwebDeepLinks path={0}, host={1}", parsedUrl.Path, parsedUrl.Hostname);

                foreach (var app in _installApps.Values)
                {
                    if (app.Dweb_deeplinks.Contains(string.Format("dweb:{0}", parsedUrl.Path)))
                    {
                        /// 加上///的原因：不修改url的话，不符合url标准，会异常，加上///可以进行路由匹配
                        request.Url = request.Url.Replace("dweb:", "dweb:///");
                        var connectResult = await _connectTo(fromMM, app.Mmid, request);
                        return await connectResult.IpcForFromMM.Request(request);
                    }
                }

                return new PureResponse(HttpStatusCode.BadGateway, Body: new PureUtf8StringBody(request.Url));
            }

            return null;
        });

        OnAfterShutdown += async (_) => { cb(); deeplinkCb(); };

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
        Event ??= IpcEvent.FromUtf8("activity", "");

        /// 启动 boot 模块
        await Open("boot.sys.dweb");
        var ipc = await ConnectAsync("boot.sys.dweb");

        if (ipc is not null)
        {
            await ipc.PostMessageAsync(Event);
        }
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
    public void Install(MicroModule mm) => _installApps.TryAdd(mm.Mmid, mm);

    /** <summary>卸载应用</summary> */
    public bool UnInstall(MicroModule mm) => _installApps.Remove(mm.Mmid);

    /** <summary>查询应用</summary> */
    public async Task<MicroModule?> Query(Mmid mmid) => _installApps.GetValueOrDefault(mmid);

    public IEnumerable<MicroModule> Search(MicroModuleCategory category)
    {
        foreach (var app in _installApps.Values)
        {
            if (app.Categories.Contains(category))
            {
                yield return app;
            }
        }
    }

    /** <summary>打开应用</summary> */
    private Task<PromiseOut<MicroModule>> _Open(Mmid mmid)
    {
        return _runningApps.GetValueOrPutAsync(mmid, async () =>
        {
            var po = new PromiseOut<MicroModule>();

            var openingMM = await Query(mmid);
            _ = Task.Run(async () =>
            {
                if (openingMM is not null)
                {
                    await BootstrapMicroModule(openingMM);
                    openingMM.OnAfterShutdown += async (_) =>
                    {
                        await _removeRunningApps(mmid);
                    };
                    po.Resolve(openingMM);
                }
            }).NoThrow();

            return po;
        });
    }
    /** <summary>打开应用</summary> */
    public async Task<MicroModule> Open(Mmid mmid) => await (await _Open(mmid)).WaitPromiseAsync();

    /** <summary>关闭应用</summary> */
    public async Task<int> Close(Mmid mmid)
    {
        var microModule = await _removeRunningApps(mmid);

        if (microModule is null)
        {
            return -1;
        }

        try
        {
            await microModule.ShutdownAsync();

            return 1;
        }
        catch (Exception e)
        {
            Console.Warn("Close", "exception: {0}", e.Message);
            return 0;
        }
    }

    private async Task<MicroModule?> _removeRunningApps(Mmid mmid)
    {
        var microModulePo = _runningApps.GetValueOrDefault(mmid);

        if (microModulePo is null)
        {
            return null;
        }

        var app = await microModulePo.WaitPromiseAsync();

        if (app is not null)
        {
            _runningApps.Remove(mmid);
        }

        return app;
    }
}

