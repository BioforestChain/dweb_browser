
namespace micro_service.sys.http.net;

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

public struct HttpServerInfo : IServerInfo<HttpClient>
{
    public HttpClient Server { get; set; }
    public string Host { get; set; }
    public string Hostname { get; set; }
    public int Port { get; set; }
    public string Origin { get; set; }
    public IProtocol Protocol { get; set; }

    public HttpServerInfo(HttpClient server, string host, string hostname, int port, string origin, IProtocol protocol)
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
	public static IServerInfo<HttpClient> HttpCreateServer(ListenOptions listenOptions)
	{
		var host = $"{listenOptions.Hostname}:{listenOptions.Port}";
		var origin = $"http://{host}";

		var httpClient = new HttpClient { BaseAddress = new Uri(origin) };

        return new HttpServerInfo(
            httpClient,
            host,
            listenOptions.Hostname,
            listenOptions.Port,
            origin,
            new HttpProtocol("http://", "http:", 80));
	}
	
}

