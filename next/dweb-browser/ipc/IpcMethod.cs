using System.Net.Http;

namespace ipc;

public class IpcMethod
{
    private readonly string _method;

    private static readonly IpcMethod s_getMethod = new IpcMethod("GET");

    private static readonly IpcMethod s_putMethod = new IpcMethod("PUT");

    private static readonly IpcMethod s_postMethod = new IpcMethod("POST");

    private static readonly IpcMethod s_deleteMethod = new IpcMethod("DELETE");

    private static readonly IpcMethod s_headMethod = new IpcMethod("HEAD");

    private static readonly IpcMethod s_optionsMethod = new IpcMethod("OPTIONS");

    private static readonly IpcMethod s_traceMethod = new IpcMethod("TRACE");

    private static readonly IpcMethod s_patchMethod = new IpcMethod("PATCH");

    private static readonly IpcMethod s_connectMethod = new IpcMethod("CONNECT");

    public static IpcMethod Get => s_getMethod;

    public static IpcMethod Put => s_putMethod;

    public static IpcMethod Post => s_postMethod;

    public static IpcMethod Delete => s_deleteMethod;

    public static IpcMethod Head => s_headMethod;

    public static IpcMethod Options => s_optionsMethod;

    public static IpcMethod Trace => s_traceMethod;

    public static IpcMethod Patch => s_patchMethod;

    public static IpcMethod Connect => s_connectMethod;

    public string method => _method;

    public IpcMethod(string method)
    {
        _method = method;
    }

    public static IpcMethod From(HttpMethod method)
    {
        return new IpcMethod(method.Method);
    }

    public override string ToString()
    {
        return _method;
    }
}
