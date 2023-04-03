
namespace micro_service.sys.http.net;

public class Http1Server
{
	public readonly static string PREFIX = "http://";
	public readonly static string PROTOCOL = "http:";
	public readonly static int PORT = 80;

	private int _bindingPort = -1;

	private IServerInfo<HttpClient>? _info;

	public IServerInfo<HttpClient>? Info
	{
		get => _info;
	}

	public string Authority
	{
		get => $"localhost:{_bindingPort}";
	}

	public string Origin
	{
		get => $"{PREFIX}{Authority}";
	}

	public Task<IServerInfo<HttpClient>> Create()
	{
		int local_port = _bindingPort = PortHelper.FindPort(new int[] { 22605 });

		return Task.Run(() =>
		{
			return (NetServer.HttpCreateServer(new ListenOptions(local_port)));
        });
	}

	public void Destroy()
	{
		_info?.Server.Dispose();
		_info = null;
	}
}

