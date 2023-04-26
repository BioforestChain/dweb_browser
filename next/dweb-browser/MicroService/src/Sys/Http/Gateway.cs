using System.Collections.Concurrent;
namespace DwebBrowser.MicroService.Sys.Http;

public class Gateway
{
    public PortListener Listener { get; init; }
    public HttpNMM.ServerUrlInfo UrlInfo { get; init; }
    public string Token { get; init; }

    public Gateway(PortListener listener, HttpNMM.ServerUrlInfo urlInfo, string token)
    {
        Listener = listener;
        UrlInfo = urlInfo;
        Token = token;
    }

    public class PortListener
    {
        public Ipc Ipc { get; init; }
        public string Host { get; init; }

        // TODO: 暂时使用ConcurrentDictionary 替代 ConcurrentSet
        private ConcurrentDictionary<StreamIpcRouter, bool> _routerSet = new();

        public PortListener(Ipc ipc, string host)
        {
            Ipc = ipc;
            Host = host;
        }

        public Func<bool> AddRouter(RouteConfig config, ReadableStreamIpc streamIpc)
        {
            var route = new StreamIpcRouter(config, streamIpc);
            _routerSet.TryAdd(route, true);

            return () => _routerSet.TryRemove(route, out bool value);
        }

        /**
         * <summary>
         * 接收 nodejs-web 请求
         * 将之转发给 IPC 处理，等待远端处理完成再代理响应回去
         * </summary>
         */
        public async Task<HttpResponseMessage?> HookHttpRequestAsync(HttpRequestMessage request)
        {

            foreach (var router in _routerSet)
            {
                var response = await router.Key.Handler(request).ForAwait(default);

                if (response is not null)
                {
                    return response;
                }
            }

            return null;
        }

        // 销毁
        public event Signal? OnDestory;

        public async Task DestroyAsync()
        {
            _routerSet.Clear();
            await (OnDestory?.Emit()).ForAwait();
        }
    }

    public record RouteConfig(string pathname, IpcMethod method, MatchMode matchMode = MatchMode.PREFIX)
    {

        public bool IsMatch(HttpRequestMessage request) => matchMode switch
        {
            MatchMode.PREFIX => request.Method.Method == method.Method && request.RequestUri is not null
                       && request.RequestUri.AbsolutePath.StartsWith(pathname),
            MatchMode.FULL => request.Method.Method == method.Method && request.RequestUri is not null
                       && request.RequestUri.AbsolutePath == pathname,
            _ => false
        };
    }

    public class StreamIpcRouter
    {
        public RouteConfig Config { get; init; }
        public ReadableStreamIpc StreamIpc { get; init; }

        public StreamIpcRouter(RouteConfig config, ReadableStreamIpc streamIpc)
        {
            Config = config;
            StreamIpc = streamIpc;

            switch (Config.matchMode)
            {
                case MatchMode.PREFIX:
                    IsMatch = (request) =>
                    {
                        return request.Method.Method == Config.method.Method && request.RequestUri is not null
                            && request.RequestUri.AbsolutePath.StartsWith(Config.pathname);
                    };
                    break;
                case MatchMode.FULL:
                    IsMatch = (request) =>
                    {
                        return request.Method.Method == Config.method.Method && request.RequestUri is not null
                            && request.RequestUri.AbsolutePath == Config.pathname;
                    };
                    break;
            }
        }

        public Func<HttpRequestMessage, bool> IsMatch { get; init; }

        public async Task<HttpResponseMessage?> Handler(HttpRequestMessage request)
        {
            if (IsMatch(request))
            {
                return await StreamIpc.Request(request);
            }
            else if (request.Method == HttpMethod.Options)
            {
                return new HttpResponseMessage(HttpStatusCode.OK).Also(it =>
                {
                    it.Headers.Add("Access-Control-Allow-Methods", "*");
                    it.Headers.Add("Access-Control-Allow-Origin", "*");
                    it.Headers.Add("Access-Control-Allow-Headers", "*");
                });
            }
            else
            {
                return null;
            }
        }
    }
}

