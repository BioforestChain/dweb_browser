
namespace DwebBrowser.MicroService.Http;

public record PureRequest(
    string Url,
    IpcMethod Method,
    IpcHeaders? Headers = null,
    PureBody? Body = null
) : System.IDisposable
{
    static readonly Debugger Console = new("PureRequest");

    public IpcHeaders Headers = Headers ?? new();
    public PureBody Body = Body ?? PureBody.Empty;

    string _url = Url;
    public string Url
    {
        get => _url;
        set
        {
            if (_url != value)
            {
                _url = value;
                _ParsedUrl.Reset();
            }
        }
    }

    LazyBox<URL?> _ParsedUrl = new();
    public URL? ParsedUrl
    {
        get => _ParsedUrl.GetOrPut(() =>
        {
            if (Uri.TryCreate(Url, UriKind.Absolute, out var parsedUrl) is false)
            {
                Console.Log("ParsedUrl", "TryParse Failed: {0}", Url);
                return null;
            }
            return new URL(parsedUrl);
        });
        set
        {
            if (value?.Uri.AbsoluteUri is var new_url && _url != new_url)
            {
                _url = new_url ?? "";
                _ParsedUrl.SetValue(value);
            };
        }
    }
    public URL SafeUrl { get => ParsedUrl ?? throw new FormatException("invalid url: " + Url); }

    public void Dispose()
    {
        if (Body is PureStreamBody streamBody)
        {
            streamBody.Dispose();
        }
    }
}
