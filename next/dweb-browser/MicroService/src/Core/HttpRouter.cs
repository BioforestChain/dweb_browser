
using DwebBrowser.MicroService.Http;
using static DwebBrowser.MicroService.Sys.Http.Gateway;

namespace DwebBrowser.MicroService.Core;

public class HttpRouter
{
    static Debugger Console = new Debugger("HttpRouter");
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

    // public async Task RouterHandler(HttpListenerContext context, Ipc? ipc = null)
    // {
    //     var request = context.Request;
    //     var response = context.Response;

    //     var result = await RouterHandler(request.ToHttpRequestMessage(), ipc);

    //     if (result is not null && response is not null)
    //     {
    //         switch (result)
    //         {
    //             case HttpResponseMessage res:
    //                 (await res.ToHttpListenerResponse(response)).Close();
    //                 break;
    //             case byte[] byteResult:
    //                 response.StatusCode = (int)HttpStatusCode.OK;
    //                 Stream outputStream = response.OutputStream;
    //                 outputStream.Write(byteResult, 0, byteResult.Length);
    //                 outputStream.Close();
    //                 response.Close();
    //                 break;
    //             case Stream stream:
    //                 response.StatusCode = (int)HttpStatusCode.OK;
    //                 stream.CopyTo(response.OutputStream);
    //                 response.Close();
    //                 break;
    //             default:
    //                 response.StatusCode = (int)HttpStatusCode.OK;
    //                 response.Close();
    //                 break;
    //         }
    //     }
    //     else
    //     {
    //         response!.StatusCode = (int)HttpStatusCode.NotFound;
    //         response.Close();
    //     }
    // }

    public async Task<object?> RouterHandler(PureRequest request, Ipc? ipc)
    {
        foreach (var (route, handler) in _routes)
        {
            if (route.IsMatch(request))
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
            null => new PureResponse(HttpStatusCode.OK),
            Unit => new PureResponse(HttpStatusCode.OK),
            PureResponse pureResponse => pureResponse,
            byte[] byteArrayData => new PureResponse(HttpStatusCode.OK, Body: new PureByteArrayBody(byteArrayData)),
            Stream streamData => new PureResponse(HttpStatusCode.OK, Body: new PureStreamBody(streamData)),
            string stringData => new PureResponse(HttpStatusCode.OK, Body: new PureUtf8StringBody(stringData)),
            _ => ResponseRegistry.Handler(res),
        };
    }
}

