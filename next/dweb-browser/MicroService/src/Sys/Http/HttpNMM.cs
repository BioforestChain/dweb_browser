using System.Diagnostics;
using System.Text.RegularExpressions;
using DwebBrowser.MicroService.Sys.Dns;

namespace DwebBrowser.MicroService.Sys.Http;

public class HttpNMM : NativeMicroModule
{
    public static Http1Server DwebServer = new Http1Server();
    public static readonly string X_DWEB_HREF = "/X-Dweb-Href/";
    /// 注册的域名与对应的 token
    private Dictionary</* token */string, Gateway> _tokenMap = new();
    private Dictionary</* host */string, Gateway> _gatewayMap = new();

    public HttpNMM() : base("http.sys.dweb")
    {
    }


    /**
     * <summary>
     * 监听请求
     *
     * 真实过来的请求有两种情况：
     * 1. http://subdomain.localhost:24433
     * 2. http://localhost:24433
     * 前者是桌面端自身 chrome 支持的情况，后者才是常态。
     * 但是我们返回给开发者的端口只有一个，这就意味着我们需要额外手段进行路由
     *
     * 如果这个请求是发生在 nativeFetch 中，我们会将请求的 url 改成 http://localhost:24433，同时在 headers.user-agent 的尾部加上 dweb-host/subdomain.localhost:24433
     * 如果这个请求是 webview 中发出，我们一开始就会为整个 webview 设置 user-agent，使其行为和上条一致
     *
     * 如果在 webview 中，要跨域其它请求，那么 webview 的拦截器能对 get 请求进行简单的转译处理，
     * 否则其它情况下，需要开发者自己用 fetch 接口来发起请求。
     * 这些自定义操作，都需要在 header 中加入 X-Dweb-Host 字段来指明宿主
     * </summary>
     */
    private async Task<HttpResponseMessage> _httpHandler(HttpRequestMessage request)
    {
        if (request.RequestUri?.AbsolutePath.StartsWith(X_DWEB_HREF) is true)
        {
            request.RequestUri = new Uri(request.RequestUri.AbsolutePath.Substring(X_DWEB_HREF.Length));
        }


        string? header_host = null;
        string? header_x_dweb_host = null;
        string? header_user_agent_host = null;
        string? query_x_web_host = request.RequestUri?.GetQuery("X-Dweb-Host")?.DecodeURIComponent();

        foreach (var entry in request.Headers)
        {
            switch (entry.Key)
            {
                case "Host":
                    header_host = entry.Value.FirstOrDefault();
                    break;
                case "X-Dweb-Host":
                    header_x_dweb_host = entry.Value.FirstOrDefault();
                    break;
                case "User-Agent":
                    header_user_agent_host = _dwebHostRegex(entry.Value.FirstOrDefault());
                    break;
            }
        }

        var host = (query_x_web_host ?? header_x_dweb_host ?? header_user_agent_host ?? header_host).Let(host =>
        {
            if (host is null) return "*";

            /// 如果没有端口，补全端口
            if (!host.Contains(':'))
            {
                return host + ":" + Http1Server.PORT;
            }

            return host;
        });

        /// TODO 这里提取完数据后，应该把header、query、uri重新整理一下组成一个新的request会比较好些
        /// TODO 30s 没有任何 body 写入的话，认为网关超时

        /**
         * WARNING 我们底层使用 KtorCIO，它是完全以流的形式来将response的内容传输给web
         * 所以这里要小心，不要去读取 response 对象，否则 pos 会被偏移
         */
        var response = _gatewayMap.GetValueOrDefault(host)?.Let(gateway =>
        {
            return gateway.Listener.HookHttpRequestAsync(request);
        });

        var notFound = new HttpResponseMessage(HttpStatusCode.NotFound);

        return response is not null
            ? (await response) is not null
                ? (await response)! : notFound
            : notFound;
    }

    private string? _dwebHostRegex(string? str)
    {
        if (str is null) return null;

        var reg = new Regex(@"\sdweb-host/(\S+)");
        MatchCollection matches = reg.Matches(str);

        foreach (Match match in matches)
        {
            GroupCollection groups = match.Groups;
            return groups[1].Value;
        }

        return null;
    }

    public struct ServerUrlInfo
    {
        public string Host;
        public string Internal_Origin;
        public string Public_Origin;

        public ServerUrlInfo(string host, string internal_origin, string public_origin)
        {
            Host = host;
            Internal_Origin = internal_origin;
            Public_Origin = public_origin;
        }

        public Uri BuildPublicUrl() => new Uri(Public_Origin).AppendQuery("X-Dweb-Host", Host);
        public Uri BuildInternalUrl() => new Uri(Internal_Origin);
    }

    private ServerUrlInfo _getServerUrlInfo(Ipc ipc, DwebHttpServerOptions options)
    {
        var mmid = ipc.Remote.Mmid;
        var subdomainPrefix = options.subdomain == "" || options.subdomain.EndsWith(".")
            ? options.subdomain : $"{options.subdomain}";

        var port = options.port <= 0 || options.port >= 6556
            ? throw new Exception($"invalid dweb http port: {options.port}") : options.port;

        var host = $"{subdomainPrefix}{mmid}:{port}";
        var internal_origin = $"https://{host}";
        var public_origin = DwebServer.Origin;

        return new ServerUrlInfo(host, internal_origin, public_origin);
    }

    public record ServerStartResult(string token, ServerUrlInfo urlInfo);

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        // TODO: 异步Lambada表达式无法转化为Func<HttpRequestMessage, HttpResponseMessage>
        await DwebServer.CreateAsync(request =>
        {
            return _httpHandler(request).Result;
        });

        /// 为 nativeFetch 函数提供支持
        var cb = NativeFetch.NativeFetchAdaptersManager.Append(async (fromMM, request) =>
        {
            if (request.RequestUri is not null &&
                request.RequestUri.Scheme is "http" or "https" &&
                request.RequestUri.Host.EndsWith(".dweb"))
            {
                // 无需走网络层，直接内部处理掉
                request.Headers.Add("X-Dweb-Host", request.RequestUri.GetFullAuthority(request.RequestUri.Authority));
                request.RequestUri = request.RequestUri.SetSchema("http").SetAuthority(DwebServer.Authority);
                return await _httpHandler(request);
            }

            return null;
        });
        _onAfterShutdown += async (_) => { cb(); };

        HttpRouter.AddRoute(HttpMethod.Get.Method, "/start", async (request, ipc) =>
        {
            var dwebServerOptions = new DwebHttpServerOptions(
                request.QueryValidate<int>("port", false),
                request.QueryValidate<string>("subdomain", false) ?? "");
            return _start(ipc, dwebServerOptions);
        });

        HttpRouter.AddRoute(HttpMethod.Post.Method, "/listen", async (request, _) =>
        {
            var token = request.QueryValidate<string>("token")!;
            var routes = JsonSerializer.Deserialize<List<Gateway.RouteConfig>>(request.QueryValidate<string>("routes")!);
            return _listen(token, request, routes);
        });

        HttpRouter.AddRoute(HttpMethod.Get.Method, "/close", async (request, ipc) =>
        {
            var dwebServerOptions = new DwebHttpServerOptions(
                request.QueryValidate<int>("port", false),
                request.QueryValidate<string>("subdomain", false) ?? "");
            return _close(ipc, dwebServerOptions);
        });
    }

    protected override Task _shutdownAsync() => Task.Run(() => DwebServer.CloseServer());

    protected override async Task _onActivityAsync(IpcEvent Event, Ipc ipc)
    { }

    private ServerStartResult _start(Ipc ipc, DwebHttpServerOptions options)
    {
        var serverUrlInfo = _getServerUrlInfo(ipc, options);
        if (_gatewayMap.ContainsKey(serverUrlInfo.Host))
        {
            throw new Exception($"already in listen: {serverUrlInfo.Internal_Origin}");
        }

        var listener = new Gateway.PortListener(ipc, serverUrlInfo.Host);

        /// ipc 在关闭的时候，自动释放所有的绑定
        listener.OnDestory += async (_) => { ipc.OnClose += async (_) => _close(ipc, options); };

        var token = Token.RandomCryptoString(8);

        var gateway = new Gateway(listener, serverUrlInfo, token);
        _gatewayMap.Add(serverUrlInfo.Host, gateway);
        _tokenMap.Add(token, gateway);

        return new ServerStartResult(token, serverUrlInfo);
    }

    /**
     * <summary>
     *  绑定流监听
     *  </summary>
     */
    private HttpResponseMessage _listen(
        string token,
        HttpRequestMessage request,
        List<Gateway.RouteConfig> routes)
    {
        var gateway = _tokenMap.GetValueOrDefault(token) ?? throw new Exception($"no gateway with token: {token}");
        Console.WriteLine($"LISTEN host: {gateway.UrlInfo.Host}, token: {token}");

        var streamIpc = new ReadableStreamIpc(gateway.Listener.Ipc.Remote, $"http-gateway/{gateway.UrlInfo.Host}");
        streamIpc.BindIncomeStream(request.Content.ReadAsStream());

        foreach (var routeConfig in routes)
        {
            streamIpc.OnClose += async (_) => gateway.Listener.AddRouter(routeConfig, streamIpc);
        }

        return new HttpResponseMessage(HttpStatusCode.OK).Also(it =>
                it.Content = new StreamContent(streamIpc.Stream.Stream));
    }

    private async Task<bool> _close(Ipc ipc, DwebHttpServerOptions options)
    {
        var serverUrlInfo = _getServerUrlInfo(ipc, options);

        var gateway = _gatewayMap.GetValueOrDefault(serverUrlInfo.Host);

        if (gateway is not null)
        {
            _tokenMap.Remove(gateway.Token);
            await gateway.Listener.DestroyAsync();
            return _gatewayMap.Remove(serverUrlInfo.Host);
        }
        else
        {
            return false;
        }
    }
}

