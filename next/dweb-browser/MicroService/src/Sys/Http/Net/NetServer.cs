
using static DwebBrowser.MicroService.Message.IpcBodySender;

namespace DwebBrowser.MicroService.Sys.Http.Net;

public interface IProtocol
{
    public string Protocol { get; set; }
    public string Prefix { get; set; }
    public int Port { get; set; }
}

public struct HttpProtocol : IProtocol
{
    public string Protocol { get; set; }
    public string Prefix { get; set; }
    public int Port { get; set; }

    public HttpProtocol(string protocol, string prefix, int port) : this()
    {
        Protocol = protocol;
        Prefix = prefix;
        Port = port;
    }
}

public interface IServerInfo<S>
{
    public S Server { get; set; }
    public string Host { get; set; }
    public string Hostname { get; set; }
    public int Port { get; set; }
    public string Origin { get; set; }
    public IProtocol Protocol { get; set; }
}

public struct HttpServerInfo : IServerInfo<HttpListener>
{
    public HttpListener Server { get; set; }
    public string Host { get; set; }
    public string Hostname { get; set; }
    public int Port { get; set; }
    public string Origin { get; set; }
    public IProtocol Protocol { get; set; }

    public HttpServerInfo(HttpListener server, string host, string hostname, int port, string origin, IProtocol protocol)
    {
        Server = server;
        Host = host;
        Hostname = hostname;
        Port = port;
        Origin = origin;
        Protocol = protocol;
    }
}

public record ListenOptions(int Port, string Hostname = "localhost");

public static class NetServer
{
    /// <summary>
    /// TODO 这里应该提供Stop函数来终止服务
    /// </summary>
    /// <param name="listenOptions"></param>
    /// <param name="handler"></param>
    /// <returns></returns>
    public static IServerInfo<HttpListener> HttpCreateServer(ListenOptions listenOptions, HttpHandler handler)
    {
        var host = $"{listenOptions.Hostname}:{listenOptions.Port}";
        var origin = $"http://{host}/";

        var listener = new HttpListener();
        listener.Prefixes.Add(origin);
        listener.Start();

        //listener.BeginGetContext((ar) =>
        //{
        //    var listener = (HttpListener?)ar.AsyncState;

        //    if (listener is not null)
        //    {
        //        var context = listener.EndGetContext(ar);
        //        handler(context.Request.ToHttpRequestMessage());
        //    }
        //}, listener);

        Task.Run(async () =>
        {
            while (true)
            {
                var context = await listener.GetContextAsync();

                var request = context.Request;
                var response = context.Response;

                var result = handler(request.ToHttpRequestMessage());
                result.ToHttpListenerResponse(response).Close();
            }
        });

        return new HttpServerInfo(
            listener,
            host,
            listenOptions.Hostname,
            listenOptions.Port,
            origin,
            new HttpProtocol("http://", "http:", 80));
    }

}

