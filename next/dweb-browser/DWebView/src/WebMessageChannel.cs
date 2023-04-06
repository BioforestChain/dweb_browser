namespace DwebBrowser.DWebView;


public class WebMessageChannel
{
    public readonly WebMessagePort port1;
    public readonly WebMessagePort port2;
    internal WebMessageChannel(WebMessagePort port1, WebMessagePort port2)
    {
        this.port1 = port1;
        this.port2 = port2;
    }
}
