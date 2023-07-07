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
         * 接收 nodejs-web 请求
         * 将之转发给 IPC 处理，等待远端处理完成再代理响应回去
         */
        public StreamIpcRouter? FindMatchedBind(string pathname, IpcMethod method)
        {
            foreach (var router in _routerSet.Keys)
            {
                if (router.Config.IsMatch(pathname, method))
                {
                    return router;
                }
            }

            return null;
        }

        /**
         * <summary>
         * 接收 nodejs-web 请求
         * 将之转发给 IPC 处理，等待远端处理完成再代理响应回去
         * </summary>
         */
        public async Task<PureResponse?> HookHttpRequestAsync(PureRequest request)
        {
            foreach (var router in _routerSet)
            {
                var response = await router.Key.Handler(request).ForAwait(default);

                if (response is not null)
                {
                    return response;
                }
            }
            /// 缺省的 OPTIONS 处理逻辑
            if (request.Method == IpcMethod.Options)
            {
                return new PureResponse(Headers: new IpcHeaders()
                    .Set("Access-Control-Allow-Methods", "*")
                    .Set("Access-Control-Allow-Origin", "*")
                    .Set("Access-Control-Allow-Headers", "*")
                );
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

    public record RouteConfig(string pathname, IpcMethod method, MatchMode? matchMode = MATCH_MODE.PREFIX, string? protocol = "")
    {
        public bool IsMatch(string pathname, IpcMethod method, string? protocol = "")
        {
            if (!string.IsNullOrEmpty(protocol) && !string.IsNullOrEmpty(this.protocol) && this.protocol != protocol)
            {
                return false;
            }

            return (this.method ?? IpcMethod.Get) == method &&
                    (matchMode == MATCH_MODE.FULL
                    ? pathname == this.pathname
                    : matchMode == MATCH_MODE.PREFIX && pathname.StartsWith(this.pathname));
        }
    }

    public class StreamIpcRouter
    {
        public RouteConfig Config { get; init; }
        public ReadableStreamIpc StreamIpc { get; init; }

        public StreamIpcRouter(RouteConfig config, ReadableStreamIpc streamIpc)
        {
            Config = config;
            StreamIpc = streamIpc;
        }


        public async Task<PureResponse?> Handler(PureRequest request)
        {
            if (Config.IsMatch(request.SafeUrl.Path, request.Method))
            {
                return await StreamIpc.Request(request);
            }
            return null;
        }
    }
}

