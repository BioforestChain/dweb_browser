namespace DwebBrowser.DWebView;


public class WebMessage
{
    public readonly NSObject Data;
    public readonly WebMessagePort[] Ports;
    public WebMessage(NSObject data, WebMessagePort[]? ports)
    {
        this.Data = data;
        this.Ports = ports ?? new WebMessagePort[] { };
    }
    public WebMessage(NSObject data)
    {
        this.Data = data;
        this.Ports = new WebMessagePort[0];
    }
    public static WebMessage From(string message, WebMessagePort[]? ports) => new WebMessage(new NSString(message), ports);
    public static WebMessage From(int message, WebMessagePort[]? ports) => new WebMessage(new NSNumber(message), ports);
    public static WebMessage From(float message, WebMessagePort[]? ports) => new WebMessage(new NSNumber(message), ports);
    public static WebMessage From(double message, WebMessagePort[]? ports) => new WebMessage(new NSNumber(message), ports);
    public static WebMessage From(bool message, WebMessagePort[]? ports) => new WebMessage(NSNumber.FromBoolean(message), ports);
}
