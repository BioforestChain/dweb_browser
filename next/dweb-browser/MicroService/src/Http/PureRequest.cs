
using System.Xml.Serialization;

namespace DwebBrowser.MicroService.Http;

public record PureRequest(
    string Url,
    IpcMethod Method,
    IpcHeaders? Headers = null,
    IPureBody? Body = null
) : IDisposable
{
    static readonly Debugger Console = new("PureRequest");

    public IpcHeaders Headers = Headers ?? new();
    public IPureBody Body = Body ?? IPureBody.Empty;

    // 用于判断是否是双工请求 websocket/http3
    public bool IsWebsocketRequest = false;
    public bool IsHttp3Request = false;
    public bool IsDuplex => IsWebsocketRequest || IsHttp3Request;

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

    readonly LazyBox<URL?> _ParsedUrl = new();
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
            GC.SuppressFinalize(this);
        }
    }
}
