
namespace micro_service.sys.http.net;

public interface IProtocol
{
	public string Protocol { get; set; }
	public string Prefix { get; set; }
	public int Port { get; set; }
}

public struct HttpProtocol : IProtocol
{
    private string v1;
    private string v2;
    private int v3;

    public HttpProtocol(string v1, string v2, int v3) : this()
    {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }

    public string Protocol { get; set; }
    public string Prefix { get; set; }
    public int Port { get; set; }
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
    private HttpClient httpClient;
    private HttpProtocol httpProtocol;

    public HttpServerInfo(HttpClient httpClient, string host, string? hostname, int port, string origin, HttpProtocol httpProtocol) : this()
    {
        this.httpClient = httpClient;
        Host = host;
        Hostname = hostname;
        Port = port;
        Origin = origin;
        this.httpProtocol = httpProtocol;
    }

    public HttpClient Server { get; set; }
    public string Host { get; set; }
    public string Hostname { get; set; }
    public int Port { get; set; }
    public string Origin { get; set; }
    public IProtocol Protocol { get; set; }
}

public record ListenOptions(int Port, string? Hostname = "localhost");

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

