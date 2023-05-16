using WebKit;
using DwebBrowser.Helper;
using UIKit;

namespace DwebBrowser.DWebView;


public class WebMessagePort
{
    internal int portId;
    private WKWebView webview;
    internal WebMessagePort(int portId, WKWebView webview)
    {
        this.portId = portId;
        this.webview = webview;
        DWebView.allPorts[portId] = this;
        //webview.EvaluateJavaScript()
    }


    public Task PostMessage(string message, WebMessagePort[]? ports = default) => PostMessage(WebMessage.From(message, ports));
    public Task PostMessage(int message, WebMessagePort[]? ports = default) => PostMessage(WebMessage.From(message, ports));
    public Task PostMessage(float message, WebMessagePort[]? ports = default) => PostMessage(WebMessage.From(message, ports));
    public Task PostMessage(double message, WebMessagePort[]? ports = default) => PostMessage(WebMessage.From(message, ports));
    public Task PostMessage(bool message, WebMessagePort[]? ports = default) => PostMessage(WebMessage.From(message, ports));
    public async Task PostMessage(WebMessage message)
    {
        var arguments = new NSDictionary<NSString, NSObject>(new NSString[] {
                new NSString("portId") ,
                new NSString("data"),
                new NSString("ports")
            }, new NSObject[] {
                new NSNumber(portId),
                message.Data,
                NSArray.FromNSObjects(message.Ports.Select(port => new NSNumber(port.portId)).ToArray())
            });
        await webview.InvokeOnMainThreadAsync(() => webview.CallAsyncJavaScriptAsync("nativePortPostMessage(portId,data,ports)", arguments, null, DWebView.webMessagePortContentWorld));
    }
    public Task Start() => webview.InvokeOnMainThreadAsync(() => webview.EvaluateJavaScriptAsync("nativeStart(" + portId + ")", null, DWebView.webMessagePortContentWorld));

    public event Signal<WebMessage>? OnMessage;
    internal Task _emitOnMessage(WebMessage msg) => (OnMessage?.Emit(msg)).ForAwait();


}
