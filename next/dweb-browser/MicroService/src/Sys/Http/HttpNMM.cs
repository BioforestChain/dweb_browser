using System.Net.WebSockets;
using System.Text.RegularExpressions;

namespace DwebBrowser.MicroService.Sys.Http;

public static class UrlExtensions
{
    public static string ToPublicDwebHref(this Uri internalHref) => HttpNMM.DwebServer.Origin + HttpNMM.X_DWEB_HREF + internalHref.AbsoluteUri;
}
public class HttpNMM : NativeMicroModule
{
    static readonly Debugger Console = new("HttpNMM");
    public static Http1Server DwebServer = new Http1Server();
    public const string X_DWEB_HREF = "/X-Dweb-Href/";
    public const string X_DWEB_HOST = "X-Dweb-Host";
    public const string INTERNAL_SCHEME = "https";
    /// <summary>
    /// 基于BuildInternalUrl拼接出来的链接，不基于Query，所以适用性更好，可以用于base-uri
    /// </summary>
    /// <param name="internalHref"></param>
    /// <returns></returns>
    /// 注册的域名与对应的 token
    private readonly Dictionary</* token */string, Gateway> _tokenMap = new();
    private readonly Dictionary</* host */string, Gateway> _gatewayMap = new();

    public override string? ShortName { get; set; } = "HTTP";
    public HttpNMM() : base("http.std.dweb", "HTTP Server Provider")
    {
    }

    public override List<MicroModuleCategory> Categories { get; init; } = new()
    {
        MicroModuleCategory.Service,
        MicroModuleCategory.Protocol_Service,
    };

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
    private string? _processHost(PureRequest request)
    {
        if (request.ParsedUrl is not null and var parsedUrl && parsedUrl.PathAndQuery.StartsWith(X_DWEB_HREF) is true)
        {
            if (Uri.TryCreate(parsedUrl.PathAndQuery.Substring(X_DWEB_HREF.Length), new(), out var newUrl))
            {
                request.ParsedUrl = new URL(newUrl);
                request.Headers.Set(X_DWEB_HOST, newUrl.Authority);
            }
        }

        string? header_host = null;
        string? header_x_dweb_host = null;
        string? header_user_agent_host = null;
        string? query_x_web_host = request.ParsedUrl?.SearchParams.Get(X_DWEB_HOST)?.DecodeURIComponent();

        foreach (var entry in request.Headers)
        {
            switch (entry.Key)
            {
                case "Host":
                    header_host = entry.Value;
                    break;
                case X_DWEB_HOST:
                    header_x_dweb_host = entry.Value;
                    break;
                case "User-Agent":
                    header_user_agent_host = _dwebHostRegex(string.Join(" ", entry.Value));
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

        return host;
    }

    private async Task<PureResponse> _httpHandler(PureRequest request)
    {
        var host = _processHost(request);

        /// TODO 这里提取完数据后，应该把header、query、uri重新整理一下组成一个新的request会比较好些
        /// TODO 30s 没有任何 body 写入的话，认为网关超时

        /**
         * WARNING 我们底层使用 KtorCIO，它是完全以流的形式来将response的内容传输给web
         * 所以这里要小心，不要去读取 response 对象，否则 pos 会被偏移
         */
        var response = await (_gatewayMap.GetValueOrDefault(host)?.Listener.HookHttpRequestAsync(request)).ForAwait(default);

        return response ?? new PureResponse(HttpStatusCode.NotFound);
    }

    private async Task _websocketHandler(PureRequest request, HttpListenerWebSocketContext webSocketContext)
    {
        var host = _processHost(request);

        await (_gatewayMap.GetValueOrDefault(host)?.Listener.HookWsRequestAsync(request, webSocketContext)).ForAwait();
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
        [JsonPropertyName("host")]
        public string Host { get; set; }
        [JsonPropertyName("internal_origin")]
        public string Internal_Origin { get; set; }
        [JsonPropertyName("public_origin")]
        public string Public_Origin { get; set; }

        public ServerUrlInfo(string host, string internal_origin, string public_origin)
        {
            Host = host;
            Internal_Origin = internal_origin;
            Public_Origin = public_origin;
        }

        public Uri BuildPublicUrl()
        {
            var url = new URL(Public_Origin);
            url.SearchParams.Set(X_DWEB_HOST, Host);
            return url.Uri;
        }
        public Uri BuildInternalUrl() => new(Internal_Origin);
    }

    private ServerUrlInfo _getServerUrlInfo(Ipc ipc, DwebHttpServerOptions options)
    {
        var mmid = ipc.Remote.Mmid;
        var subdomainPrefix = options.Subdomain == "" || options.Subdomain.EndsWith(".")
            ? options.Subdomain : options.Subdomain + ".";

        var port = options.Port <= 0 || options.Port >= 65536
            ? throw new Exception(string.Format("invalid dweb http port: {0}", options.Port)) : options.Port;

        var host = string.Format("{0}{1}:{2}", subdomainPrefix, mmid, port);
        var internal_origin = string.Format("{0}://{1}", INTERNAL_SCHEME, host);
        var public_origin = DwebServer.Origin;

        return new ServerUrlInfo(host, internal_origin, public_origin);
    }

    public record ServerStartResult(string token, ServerUrlInfo urlInfo);

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        /// 启动http后端服务
        DwebServer.CreateServer(_httpHandler, _websocketHandler);

        /// 为 nativeFetch 函数提供支持
        var cb = NativeFetch.NativeFetchAdaptersManager.Append(async (fromMM, request) =>
        {
            if (request.ParsedUrl is not null and var parsedUrl &&
                parsedUrl.Scheme is "http" or "https" &&
                parsedUrl.Hostname.EndsWith(".dweb"))
            {
                // 无需走网络层，直接内部处理掉
                if (request.Headers.Has(X_DWEB_HOST) is false)
                {
                    request.Headers.Set(X_DWEB_HOST, parsedUrl.FullHost);
                }
                return await _httpHandler(request);
            }

            return null;
        });
        OnAfterShutdown += async (_) => { cb(); };

        HttpRouter.AddRoute(IpcMethod.Get, "/start", async (request, ipc) =>
        {
            var searchParams = request.SafeUrl.SearchParams;
            var dwebServerOptions = new DwebHttpServerOptions(
                searchParams.Get("port")?.ToIntOrNull(),
                searchParams.Get("subdomain"));
            return _start(ipc!, dwebServerOptions);
        });

        HttpRouter.AddRoute(IpcMethod.Post, "/listen", async (request, _) =>
        {
            var searchParams = request.SafeUrl.SearchParams;
            var token = searchParams.ForceGet("token")!;
            var routes = searchParams.ForceGet("routes")!;
            var routesType = typeof(List<Gateway.RouteConfig>)!;
            var gatewayRoutes = (List<Gateway.RouteConfig>)JsonSerializer.Deserialize(routes, routesType)!;
            return _listen(token, request, gatewayRoutes);
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/close", async (request, ipc) =>
        {
            var searchParams = request.SafeUrl.SearchParams;
            var dwebServerOptions = new DwebHttpServerOptions(
                searchParams.Get("port")?.ToIntOrNull(),
                searchParams.Get("subdomain"));
            return await _close(ipc!, dwebServerOptions);
        });
        /// HTTP-GET 请求，但是不是通过网关，直接走IPC
        HttpRouter.AddRoute(new Gateway.RouteConfig(X_DWEB_HREF, IpcMethod.Get, MATCH_MODE.PREFIX), async (request, _) =>
        {
            return await _httpHandler(request);
        });
    }

    protected override async Task _shutdownAsync() => DwebServer.CloseServer();

    private ServerStartResult _start(Ipc ipc, DwebHttpServerOptions options)
    {
        var serverUrlInfo = _getServerUrlInfo(ipc, options);
        if (_gatewayMap.ContainsKey(serverUrlInfo.Host))
        {
            throw new Exception(string.Format("already in listen: {0}", serverUrlInfo.Internal_Origin));
        }

        var listener = new Gateway.PortListener(ipc, serverUrlInfo.Host);

        /// ipc 在关闭的时候，自动释放所有的绑定
        listener.OnDestory += async (_) =>
        {
            await _close(ipc, options);
        };
        ipc.OnClose += async (_) =>
        {
            await listener.DestroyAsync();
        };

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
    private PureResponse _listen(
        string token,
        PureRequest request,
        List<Gateway.RouteConfig> routes)
    {
        var gateway = _tokenMap.GetValueOrDefault(token) ?? throw new Exception(string.Format("no gateway with token: {0}", token));
        Console.Log("Listen", "host: {0}, token: {1}", gateway.UrlInfo.Host, token);

        var streamIpc = new ReadableStreamIpc(gateway.Listener.Ipc.Remote, string.Format("http-gateway/{0}", gateway.UrlInfo.Host));
        /// 接收一个body，body在关闭的时候，fetchIpc也会一同关闭
        streamIpc.BindIncomeStream(request.Body.ToStream());
        /// 自己nmm销毁的时候，ipc也会被全部销毁
        this.AddToIpcSet(streamIpc);
        /// 自己创建的，就要自己销毁：这个listener被销毁的时候，streamIpc也要进行销毁
        gateway.Listener.OnDestory += (_) => streamIpc.Close();

        foreach (var routeConfig in routes)
        {
            var offRouter = gateway.Listener.AddRouter(routeConfig, streamIpc);
            streamIpc.OnClose += async (_) => offRouter();
        }

        return new PureResponse(Body: new PureStreamBody(streamIpc.ReadableStream.Stream));
    }

    private async Task<bool> _close(Ipc ipc, DwebHttpServerOptions options)
    {
        var serverUrlInfo = _getServerUrlInfo(ipc, options);

        var gateway = _gatewayMap.GetValueOrDefault(serverUrlInfo.Host);

        if (gateway is not null)
        {
            var success = _gatewayMap.Remove(serverUrlInfo.Host);
            _tokenMap.Remove(gateway.Token);
            await gateway.Listener.DestroyAsync();
            return success;
        }
        else
        {
            return false;
        }
    }
}

