using System.Net;
using micro_service.sys.http.net;

namespace micro_service.sys.http;

public class HttpNMM : NativeMicroModule
{
    public static Http1Server DwebServer { get; set; }
    public override string Mmid { get; init; }

    /// 注册的域名与对应的 token
    private Dictionary</* token */string, Gateway> _tokenMap = new();
    private Dictionary</* host */string, Gateway> _gatewayMap = new();

    public HttpNMM()
    {
        Mmid = "http.sys.dweb";
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

    public static Func<HttpRequestMessage, HttpResponseMessage> DefineHandler(
        Func<HttpRequestMessage, object?> handler)
    {
        return request =>
        {
            switch (handler(request))
            {
                case null:
                    return new HttpResponseMessage(HttpStatusCode.OK);
                case HttpResponseMessage response:
                    return response;
                case byte[] result:
                    return new HttpResponseMessage(HttpStatusCode.OK).Also(it =>
                    {
                        it.Content = new StreamContent(new MemoryStream().Let(s =>
                        {
                            s.Write(result, 0, result.Length);
                            return s;
                        }));
                    });
                case Stream stream:
                    return new HttpResponseMessage(HttpStatusCode.OK).Also(it => it.Content = new StreamContent(stream));
                default:
                    return new HttpResponseMessage(HttpStatusCode.OK);
            }
        };
    }

    public static Func<HttpRequestMessage, HttpResponseMessage> DefineHandler(
        Func<HttpRequestMessage, Ipc, object?> handler, Ipc ipc) =>
        DefineHandler(request => handler(request, ipc));

    protected override Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        throw new NotImplementedException();
    }

    protected override Task _shutdownAsync()
    {
        throw new NotImplementedException();
    }

    protected override Task _onActivityAsync(IpcEvent Event, Ipc ipc)
    {
        throw new NotImplementedException();
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

