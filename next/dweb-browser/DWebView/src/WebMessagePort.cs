using System.Text.Json;
using DwebBrowser.Helper;
using WebKit;

namespace DwebBrowser.DWebView;


public class WebMessagePort
{
    static readonly Debugger Console = new("WebMessagePort");
    internal int portId;
    private readonly WKWebView webview;
    internal WebMessagePort(int portId, WKWebView webview)
    {
        this.portId = portId;
        this.webview = webview;
        DWebView.allPorts[portId] = this;
        //webview.EvaluateJavaScript()
    }


    //public Task PostMessage(string message, WebMessagePort[]? ports = default) => PostMessage(WebMessage.From(message, ports)).NoThrow();
    public Task PostMessage(int message, WebMessagePort[]? ports = default) => PostMessage(WebMessage.From(message, ports)).NoThrow();
    public Task PostMessage(float message, WebMessagePort[]? ports = default) => PostMessage(WebMessage.From(message, ports)).NoThrow();
    public Task PostMessage(double message, WebMessagePort[]? ports = default) => PostMessage(WebMessage.From(message, ports)).NoThrow();
    public Task PostMessage(bool message, WebMessagePort[]? ports = default) => PostMessage(WebMessage.From(message, ports)).NoThrow();
    public async Task PostMessage(WebMessage message)
    {
        var arguments = new NSDictionary<NSString, NSObject>(new NSString[] {
                new NSString("portId") ,
                new NSString("data"),
                new NSString("ports")
            }, new NSObject[] {
                new NSNumber(portId),
                message.Data,
                //NSArray.FromNSObjects(message.Ports.Select(port => new NSNumber(port.portId)).ToArray())
                NSArray.FromNSObjects(Array.ConvertAll(message.Ports, port => new NSNumber(port.portId)))
            });
        await webview.InvokeOnMainThreadAsync(() => webview.CallAsyncJavaScriptAsync("nativePortPostMessage(portId,data,ports)", arguments, null, DWebView.webMessagePortContentWorld));
    }

    public async Task PostMessage(string message, WebMessagePort[]? ports = default)
    {
        var portIdList = ports == null ? "" : string.Join(",", Array.ConvertAll(ports, port => port.portId));
        //Console.Log("webview", $@"nativePortPostMessage({portId},{message}, [{portIdList}])");
        webview.InvokeOnMainThread(() => webview.EvaluateJavaScript($@"nativePortPostMessage({portId},{JsonSerializer.Serialize(message)}, [{portIdList}])", completionHandler: null, frame: null, contentWorld: DWebView.webMessagePortContentWorld));
    }
    public async Task PostMessage(byte[] message, WebMessagePort[]? ports = default)
    {
        var portIdList = ports == null ? "" : string.Join(",", Array.ConvertAll(ports, port => port.portId));
        webview.InvokeOnMainThread(() => webview.EvaluateJavaScript($"nativePortPostMessage({portId},[{string.Join(",", message)}], [{portIdList}])", completionHandler: null, frame: null, contentWorld: DWebView.webMessagePortContentWorld));
    }

    public Task Start() => webview.InvokeOnMainThreadAsync(() => webview.EvaluateJavaScriptAsync("nativeStart(" + portId + ")", null, DWebView.webMessagePortContentWorld));

    private readonly HashSet<Signal<WebMessage>> MessageSignal = new();
    public event Signal<WebMessage> OnMessage
    {
        add { if (value != null) lock (MessageSignal) { MessageSignal.Add(value); } }
        remove { lock (MessageSignal) { MessageSignal.Remove(value); } }
    }
    internal Task EmitOnMessage(WebMessage msg) => (MessageSignal.Emit(msg)).ForAwait();

    public async Task Close()
    {
        var arguments = new NSDictionary<NSString, NSObject>(new NSString[] { new NSString("portId"), }, new NSObject[] { new NSNumber(portId), });
        await webview.InvokeOnMainThreadAsync(() => webview.CallAsyncJavaScriptAsync("nativeClose(portId)", arguments, null, DWebView.webMessagePortContentWorld));
    }
}
