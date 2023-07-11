namespace DwebBrowser.DWebView;


public class WebMessage
{
    public readonly NSObject Data;
    public readonly WebMessagePort[] Ports;
    public WebMessage(NSObject data, WebMessagePort[]? ports)
    {
        Data = data;
        Ports = ports ?? Array.Empty<WebMessagePort>();
    }
    public WebMessage(NSObject data)
    {
        Data = data;
        Ports = Array.Empty<WebMessagePort>();
    }
    public static WebMessage From(string message, WebMessagePort[]? ports = default) => new(new NSString(message), ports);
    public static WebMessage From(int message, WebMessagePort[]? ports = default) => new(new NSNumber(message), ports);
    public static WebMessage From(float message, WebMessagePort[]? ports = default) => new(new NSNumber(message), ports);
    public static WebMessage From(double message, WebMessagePort[]? ports = default) => new(new NSNumber(message), ports);
    public static WebMessage From(bool message, WebMessagePort[]? ports = default) => new(NSNumber.FromBoolean(message), ports);
}
