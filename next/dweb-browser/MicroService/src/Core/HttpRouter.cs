using static DwebBrowser.MicroService.Sys.Http.Gateway;

namespace DwebBrowser.MicroService.Core;

public class HttpRouter
{
    static readonly Debugger Console = new("HttpRouter");
    private readonly Dictionary<RouteConfig, RouterHandlerType> _routes = new();

    public void AddRoute(IpcMethod method, string path, RouterHandlerType handler) =>
        AddRoute(new RouteConfig(path, method, Sys.Http.MATCH_MODE.FULL), handler);

    public void AddRoute(RouteConfig config, RouterHandlerType handler)
    {
        _routes[config] = handler;
    }
    public void ClearRoutes()
    {
        _routes.Clear();
    }

    public async Task<object?> RouterHandler(PureRequest request, Ipc? ipc)
    {
        foreach (var (route, handler) in _routes)
        {
            if (route.IsMatch(request.ParsedUrl?.Path ?? "/", request.Method))
            {
                var res = await handler(request, ipc);
                Console.Log("RouterHandler", "res: {0}", res);
                return res;
            }
        }
        return null;
    }

    public async Task<PureResponse> RoutesWithContext(PureRequest request, Ipc ipc)
    {
        object? res;
        return (res = await RouterHandler(request, ipc)) switch
        {
            null or Unit => new PureResponse(HttpStatusCode.OK),
            PureResponse pureResponse => pureResponse,
            byte[] byteArrayData => new PureResponse(HttpStatusCode.OK, Body: new PureByteArrayBody(byteArrayData)),
            Stream streamData => new PureResponse(HttpStatusCode.OK, Body: new PureStreamBody(streamData)),
            string stringData => new PureResponse(HttpStatusCode.OK, Body: new PureUtf8StringBody(stringData)),
            _ => ResponseRegistry.Handler(res),
        };
    }
}

