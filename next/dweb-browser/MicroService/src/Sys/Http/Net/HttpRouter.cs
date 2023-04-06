using System;
namespace DwebBrowser.MicroService.Sys.Http.Net;



public static class HttpRouter
{
    private static readonly Dictionary<string, Dictionary<string, RouterHandlerType>> _routes = new();

    public static void AddRoute(string method, string path, RouterHandlerType handler)
    {
        if (!_routes.ContainsKey(method))
        {
            _routes[method] = new Dictionary<string, RouterHandlerType>();
        }

        _routes[method][path] = handler;
    }

    public static async Task RouterHandler(HttpListenerContext context, Ipc? ipc = null)
    {
        var request = context.Request;
        var response = context.Response;

        var result = await RouterHandler(request.ToHttpRequestMessage(), ipc);

        if (result is not null && response is not null)
        {
            switch (result)
            {
                case HttpResponseMessage res:
                    res.ToHttpListenerResponse(response).Close();
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

    public static async Task<object?> RouterHandler(HttpRequestMessage request, Ipc? ipc)
    {
        if (_routes.TryGetValue(request.Method.Method, out var methodRoutes) && request.RequestUri is not null &&
           methodRoutes.TryGetValue(request.RequestUri.AbsolutePath, out var handler))
        {
            return await handler(request, ipc);
        }
        else
        {
            return null;
        }
    }
}

