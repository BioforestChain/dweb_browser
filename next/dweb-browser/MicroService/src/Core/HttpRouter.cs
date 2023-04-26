
using static DwebBrowser.MicroService.Sys.Http.Gateway;

namespace DwebBrowser.MicroService.Core;

public class HttpRouter
{
    private readonly Dictionary<RouteConfig, RouterHandlerType> _routes = new();

    public void AddRoute(string method, string path, RouterHandlerType handler) => AddRoute(new RouteConfig(path, IpcMethod.From(method), Sys.Http.MatchMode.FULL), handler);

    public void AddRoute(RouteConfig config, RouterHandlerType handler)
    {
        _routes[config] = handler;
    }
    public void ClearRoutes()
    {
        _routes.Clear();
    }

    public async Task RouterHandler(HttpListenerContext context, Ipc? ipc = null)
    {
        var request = context.Request;
        var response = context.Response;

        var result = await RouterHandler(request.ToHttpRequestMessage(), ipc);

        if (result is not null && response is not null)
        {
            switch (result)
            {
                case HttpResponseMessage res:
                    (await res.ToHttpListenerResponse(response)).Close();
                    break;
                case byte[] byteResult:
                    response.StatusCode = (int)HttpStatusCode.OK;
                    Stream outputStream = response.OutputStream;
                    outputStream.Write(byteResult, 0, byteResult.Length);
                    outputStream.Close();
                    response.Close();
                    break;
                case Stream stream:
                    response.StatusCode = (int)HttpStatusCode.OK;
                    stream.CopyTo(response.OutputStream);
                    response.Close();
                    break;
                default:
                    response.StatusCode = (int)HttpStatusCode.OK;
                    response.Close();
                    break;
            }
        }
        else
        {
            response.StatusCode = (int)HttpStatusCode.NotFound;
            response.Close();
        }
    }

    public async Task<object?> RouterHandler(HttpRequestMessage request, Ipc? ipc)
    {
        foreach (var (route, handler) in _routes)
        {
            if (route.IsMatch(request))
            {
                var res = await handler(request, ipc);
                Console.WriteLine(String.Format("res: {0}", res));
                return res;
            }
        }
        return null;
    }

    public async Task<HttpResponseMessage> RoutesWithContext(HttpRequestMessage request, Ipc ipc)
    {
        object? res;
        return (res = await RouterHandler(request, ipc)) switch
        {
            null => new HttpResponseMessage(HttpStatusCode.OK),
            HttpResponseMessage response => response,
            byte[] result => new HttpResponseMessage(HttpStatusCode.OK).Also(it =>
                            {
                                //it.Content = new StreamContent(new MemoryStream().Let(s =>
                                //{
                                //    s.Write(result, 0, result.Length);
                                //    return s;
                                //}));
                                it.Content = new ByteArrayContent(result);
                            }),
            Stream stream => new HttpResponseMessage(HttpStatusCode.OK).Also(it => it.Content = new StreamContent(stream)),
            _ => NativeMicroModule.ResponseRegistry.Handler(res),
        };
    }
}

