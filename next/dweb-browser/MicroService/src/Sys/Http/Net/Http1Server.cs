
namespace DwebBrowser.MicroService.Sys.Http.Net;

public class Http1Server
{
    public readonly static string PREFIX = "http://";
    public readonly static string PROTOCOL = "http:";
    public readonly static int PORT = 80;

    private int _bindingPort = -1;

    private IServerInfo<HttpListener>? _info;

    public IServerInfo<HttpListener>? Info
    {
        get => _info;
    }

    public string Authority
    {
        get => string.Format("localhost:{0}", _bindingPort);
    }

    public string Origin
    {
        get => string.Format("{0}{1}", PREFIX, Authority);
    }

    public IServerInfo<HttpListener> CreateServer(HttpHandler handler, WebSocketHandler webSocketHandler)
    {
        int local_port = _bindingPort = PortHelper.FindPort(new int[] { 22605 });

        return NetServer.HttpCreateServer(new ListenOptions(local_port), handler, webSocketHandler);
    }

    public void CloseServer()
    {
        _info?.Server.Stop();
        _info = null;
    }
}

