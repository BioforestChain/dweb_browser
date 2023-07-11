namespace DwebBrowser.DWebView;


public class WebMessageChannel
{
    public readonly WebMessagePort Port1;
    public readonly WebMessagePort Port2;
    internal WebMessageChannel(WebMessagePort port1, WebMessagePort port2)
    {
        Port1 = port1;
        Port2 = port2;
    }
}
