using Foundation;
using System;
using WebKit;
using UIKit;
using DwebBrowser.MicroService;
using DwebBrowser.Helper;
using DwebBrowser.MicroService.Sys.Dns;
using DwebBrowser.MicroService.Core;
using System.Xml;
using AngleSharp;
using AngleSharp.Html.Parser;
using DwebBrowser.MicroService.Sys.Http;
using ObjCRuntime;
using AngleSharp.Dom;
using AngleSharp.Io;
using HttpMethod = System.Net.Http.HttpMethod;
using SystemConfiguration;
using System.Runtime.Versioning;
using static CoreFoundation.DispatchSource;

namespace DwebBrowser.DWebView;

public partial class DWebView : WKWebView
{
    static Debugger Console = new Debugger("DWebView");

    MicroModule localeMM;
    MicroModule remoteMM;
    Options options;
    public class DwebSchemeHandler : NSObject, IWKUrlSchemeHandler
    {
        MicroModule microModule;
        public DwebSchemeHandler(MicroModule microModule)
        {
            this.microModule = microModule;
        }
        [Export("webView:startURLSchemeTask:")]
        [SupportedOSPlatform("ios11.0")]
        public async void StartUrlSchemeTask(WKWebView webView, IWKUrlSchemeTask urlSchemeTask)
        {
            if (urlSchemeTask.Request.HttpMethod is "GET" && urlSchemeTask.Request.Url.Path?.StartsWith(HttpNMM.X_DWEB_HREF) is true)
            {
                var request = new HttpRequestMessage(HttpMethod.Get, urlSchemeTask.Request.Url.Path.Substring(HttpNMM.X_DWEB_HREF.Length));
                //var request = new HttpRequestMessage(HttpMethod.Get, "file://http.sys.dweb" + urlSchemeTask.Request.Url.Path);
                var response = await microModule.NativeFetchAsync(request);
                //using var response = new NSHttpUrlResponse(urlSchemeTask.Request.Url, statusCode, "HTTP/1.1", dic);
                var nsStatusCode = new IntPtr((int)response.StatusCode);
                var nsHeaders = new NSMutableDictionary<NSString, NSString>();
                foreach (var (key, values) in response.Headers)
                {
                    var value = values.FirstOrDefault();
                    if (value is not null)
                    {
                        nsHeaders.Add(new NSString(key), new NSString(value));
                    }
                }

                using var nsResponse = new NSHttpUrlResponse(urlSchemeTask.Request.Url, nsStatusCode, "HTTP/1.1", nsHeaders);
                urlSchemeTask.DidReceiveResponse(nsResponse);


                // 将响应体发送到urlSchemeTask
                using (var stream = await response.StreamAsync())
                {
                    await foreach (var bytes in stream.ReadBytesStream())
                    {
                        urlSchemeTask.DidReceiveData(NSData.FromArray(bytes));
                    }
                }
                urlSchemeTask.DidFinish();
            }
            else
            {
                urlSchemeTask.DidFinish();
            }
        }

        [Export("webView:stopURLSchemeTask:")]
        public void StopUrlSchemeTask(WKWebView webView, IWKUrlSchemeTask urlSchemeTask)
        {
        }
    }

    public DWebView(CGRect frame, MicroModule localeMM, MicroModule remoteMM, Options options, WKWebViewConfiguration configuration) : base(frame, configuration.Also(configuration =>
    {
        //configuration.Preferences.SetValueForKey(NSObject.FromObject(true), new NSString("developerExtrasEnabled"));

        //configuration.SetUrlSchemeHandler(new FileSchemeHandler(remoteMM), urlScheme: "https");
        //configuration.SetUrlSchemeHandler(new FileSchemeHandler(remoteMM), urlScheme: "http");
        //configuration.SetUrlSchemeHandler(new FileSchemeHandler(remoteMM), urlScheme: "file");
        configuration.SetUrlSchemeHandler(new DwebSchemeHandler(remoteMM), urlScheme: "dweb");
    }))
    {
        this.localeMM = localeMM;
        this.remoteMM = remoteMM;
        this.options = options;

        /// 判断当前设备版本是否大于等于iOS16.4版本，大于等于的话，开启Safari调试需要开启 inspectable
        if (UIDevice.CurrentDevice.CheckSystemVersion(16, 4))
        {
            this.SetValueForKeyPath(NSNumber.FromBoolean(true), new NSString("inspectable"));
        }

        if (options.url.Length > 0)
        {
            LoadURL(options.url);
        }
    }
    public DWebView(CGRect? frame, MicroModule localeMM, MicroModule? remoteMM, Options? options, WKWebViewConfiguration? configuration) : this(frame ?? CGRect.Empty, localeMM, remoteMM ?? localeMM, options ?? Options.Empty, configuration ?? CreateDWebViewConfiguration())
    {
    }

    public DWebView(MicroModule localeMM, MicroModule? remoteMM = default, Options? options = default, CGRect? frame = default, WKWebViewConfiguration? configuration = default) : this(frame ?? CGRect.Empty, localeMM, remoteMM ?? localeMM, options ?? Options.Empty, configuration ?? CreateDWebViewConfiguration())
    {
    }

    public class Options
    {   /**
         * 要加载的页面
         */
        public string url;
        public Options(string url)
        {
            this.url = url;
        }
        public static Options Empty = new Options("");
    }


    public static WKWebViewConfiguration CreateDWebViewConfiguration()
    {
        var configuration = new WKWebViewConfiguration();
        var preferences = configuration.Preferences;
        //preferences.JavaScriptCanOpenWindowsAutomatically = true;
        preferences.JavaScriptEnabled = true;

        var webpagePreferences = configuration.DefaultWebpagePreferences ?? new WKWebpagePreferences();
        webpagePreferences.AllowsContentJavaScript = true;
        configuration.DefaultWebpagePreferences = webpagePreferences;

        return configuration;

    }
    /// <summary>
    ///  这段代码使用 MessageChannelShim.ts 文件来生成，到 https://www.typescriptlang.org/play 粘贴这个文件的代码即可
    /// </summary>
    static readonly string webMessagePortPrepareCode = $$"""
    const ALL_PORT = new Map();
    let portIdAcc = 1;
    const PORTS_ID = new WeakMap();
    const getPortId = (port) => {
        let port_id = PORTS_ID.get(port);
        if (port_id === undefined) {
            const current_port_id = portIdAcc++;
            port_id = current_port_id;
            ALL_PORT.set(port_id, port);
            port.addEventListener('message', (event) => {
                webkit.messageHandlers.webMessagePort.postMessage({
                    type: 'message',
                    id: current_port_id,
                    data: event.data,
                    ports: event.ports.map(getPortId),
                });
            });
        }
        return port_id;
    };
    function nativeCreateMessageChannel() {
        const channel = new MessageChannel();
        const port1_id = getPortId(channel.port1);
        const port2_id = getPortId(channel.port2);
        return [port1_id, port2_id];
    }
    function forceGetPort(port_id) {
        const port = ALL_PORT.get(port_id);
        if (port === undefined) {
            throw new Error(`no found messagePort by ref: ${port_id}`);
        }
        return port;
    }
    function nativePortPostMessage(port_id, data, ports_id) {
        const origin_port = forceGetPort(port_id);
        const transfer_ports = ports_id.map(forceGetPort);
        origin_port.postMessage(data, transfer_ports);
    }
    function nativeStart(port_id) {
        const origin_port = forceGetPort(port_id);
        origin_port.start();
    }
    function nativeWindowPostMessage(data, ports_id) {
        const ports = ports_id.map(forceGetPort);
        dispatchEvent(new MessageEvent('message', { data, ports }));
    }
    """;
    internal static readonly WKContentWorld webMessagePortContentWorld = WKContentWorld.Create("web-message-port");
    internal static Dictionary<int, WebMessagePort> allPorts = new Dictionary<int, WebMessagePort>();


    readonly WKScriptMessageHandler webMessagePortMessageHanlder = new WebMessagePortMessageHanlder();

    internal class WebMessagePortMessageHanlder : WKScriptMessageHandler
    {
        [Export("userContentController:didReceiveScriptMessage:")]
        public override async void DidReceiveScriptMessage(WKUserContentController userContentController, WKScriptMessage messageEvent)
        {
            var message = messageEvent.Body;
            try
            {
                var type = (string)(NSString)message.ValueForKey(new NSString("type"));
                if (type == "message")
                {
                    var id = (int)(NSNumber)message.ValueForKey(new NSString("id"));
                    var data = message.ValueForKey(new NSString("data"));
                    WebMessagePort[] ports = new WebMessagePort[0];//message.ValueForKey(new NSString("ports"));

                    var originPort = DWebView.allPorts[id] ?? throw new KeyNotFoundException();
                    await originPort._emitOnMessage(new WebMessage(data, ports));
                }
            }
            catch { }
        }
    }

    public async Task<WebMessageChannel> CreateWebMessageChannel()
    {
        /// 页面可能会被刷新，所以需要重新判断：函数可不可用
        var webMessagePortInited = (bool)(NSNumber)await base.EvaluateJavaScriptAsync("typeof nativeCreateMessageChannel==='function'", null, webMessagePortContentWorld);
        if (!webMessagePortInited)
        {
            await base.EvaluateJavaScriptAsync(new NSString(webMessagePortPrepareCode), null, webMessagePortContentWorld);
            base.Configuration.UserContentController.AddScriptMessageHandler(webMessagePortMessageHanlder, webMessagePortContentWorld, "webMessagePort");
        }
        var ports_id = (NSArray)await base.EvaluateJavaScriptAsync("nativeCreateMessageChannel()", null, webMessagePortContentWorld);

        var port1_id = (int)ports_id.GetItem<NSNumber>(0);
        var port2_id = (int)ports_id.GetItem<NSNumber>(1);
        //var port1_id = NSArray.from
        //var messagePort = new WebMessagePort();
        var port1 = new WebMessagePort(port1_id, this);
        var port2 = new WebMessagePort(port2_id, this);
        var channel = new WebMessageChannel(port1, port2);

        return channel;
    }

    public Task PostMessage(string message, WebMessagePort[]? ports = default) => PostMessage(WebMessage.From(message, ports));
    public Task PostMessage(int message, WebMessagePort[]? ports = default) => PostMessage(WebMessage.From(message, ports));
    public Task PostMessage(float message, WebMessagePort[]? ports = default) => PostMessage(WebMessage.From(message, ports));
    public Task PostMessage(double message, WebMessagePort[]? ports = default) => PostMessage(WebMessage.From(message, ports));
    public Task PostMessage(bool message, WebMessagePort[]? ports = default) => PostMessage(WebMessage.From(message, ports));
    public async Task PostMessage(WebMessage message)
    {
        var arguments = new NSDictionary<NSString, NSObject>(new NSString[] {
                new NSString("data"),
                new NSString("ports")
            }, new NSObject[] {
                message.Data,
                NSArray.FromNSObjects(message.Ports.Select(port => new NSNumber(port.portId)).ToArray())
            });
        await base.CallAsyncJavaScriptAsync("nativeWindowPostMessage(data,ports)", arguments, null, webMessagePortContentWorld);
    }
    public Task LoadURL(string url) => LoadURL(new Uri(url));

    HtmlParser htmlParser = new HtmlParser();

    public async Task LoadURL(Uri url, HttpMethod? method = default)
    {

        /// 如果是 dweb 域名，这是需要加入网关的链接前缀才能被正常加载
        if (url.Host.EndsWith(".dweb") && url.Scheme is "http" or "https")
        {
            url = new Uri(url.ToPublicDwebHref());
            //using var request = new HttpRequestMessage(method ?? HttpMethod.Get, url);
            //using var response = await remoteMM.NativeFetchAsync(request);

            //using var nsUrlResponse = new NSUrlResponse(nsUrlRequest.Url, response.Content.Headers.ContentType?.MediaType ?? "application/octet-stream", new IntPtr(response.Content.Headers.ContentLength ?? 0), response.Content.Headers.ContentType?.CharSet);
            //string responseData = await response.Content.ReadAsStringAsync() ?? "";

            //var document = htmlParser.ParseDocument(responseData);
            //var baseNode = document.Head?.QuerySelector("base");
            //if (baseNode is null)
            //{
            //    baseNode = document.CreateElement("base");
            //    document.Head!.InsertBefore(baseNode, document.Head.FirstChild);
            //}
            //string origin = baseNode.GetAttribute("href")?.Let((href) => new Uri(url, href).ToString()) ?? uri;
            //string gatewayOrigin = HttpNMM.DwebServer.Origin; // "dweb:";
            //if (!origin.StartsWith(gatewayOrigin))
            //{
            //    baseNode.SetAttribute("href", gatewayOrigin + HttpNMM.X_DWEB_HREF + origin);
            //    responseData = document.ToHtml();
            //}

            ///// 模拟加载
            //var nsData = NSData.FromString(responseData);
            //nsNavigation = LoadSimulatedRequest(nsUrlRequest, nsUrlResponse, nsData);
        }

        string uri = url.ToString() ?? throw new ArgumentException();
        var nsUrlRequest = new NSUrlRequest(new NSUrl(uri));
        WKNavigation? nsNavigation;
        nsNavigation = LoadRequest(nsUrlRequest);


        if (OnReady is not null && !OnReady.IsEmpty())
        {
            this.NavigationDelegate = new OnReadyDelegate(OnReady);
        }
    }

    public event Signal? OnReady;

    class OnReadyDelegate : WKNavigationDelegate
    {
        Signal _onReady;
        public OnReadyDelegate(Signal onReady)
        {
            this._onReady = onReady;
        }
        public override void DidStartProvisionalNavigation(WKWebView webView, WKNavigation navigation)
        {
            Console.Log("WKNavigationDelegate", "Started provisional navigation");
        }

        public override void DidFinishNavigation(WKWebView webView, WKNavigation navigation)
        {
            //base.DidFinishNavigation(webView, navigation);
            _ = _onReady.Emit();
        }
    }


    static private int idAcc = 0;
    static private Dictionary<int, PromiseOut<NSObject>> asyncTaskMap = new();
    static string JS_ASYNC_KIT = "__native_async_callback_kit__";
    static string asyncCodePrepareCode = $$"""
    {{JS_ASYNC_KIT}} = {
        resolve(id,res){
            webkit.messageHandlers.asyncCode.postMessage([1,id,res])
        },
        reject(id,err){
            webkit.messageHandlers.asyncCode.postMessage([0,id,"QQQQ:"+(err instanceof Error?(err.stack||err.message):String(err))])
        }
    };
    void 0;
    """;
    //internal static readonly WKContentWorld asyncCodeContentWorld = WKContentWorld.DefaultClient; // WKContentWorld.Create("async-code");
    readonly AsyncCodeMessageHanlder asyncCodeMessageHanlder = new();

    internal class AsyncCodeMessageHanlder : WKScriptMessageHandler
    {

        [Export("userContentController:didReceiveScriptMessage:")]
        public override void DidReceiveScriptMessage(WKUserContentController userContentController, WKScriptMessage messageEvent)
        {
            var message = (NSArray)messageEvent.Body;

            var isSuccess = (bool)message.GetItem<NSNumber>(0);
            var id = (int)message.GetItem<NSNumber>(1);
            if (asyncTaskMap.Remove(id, out var asyncTask))
            {
                if (isSuccess)
                {
                    asyncTask.Resolve(message.GetItem<NSObject>(2));
                }
                else
                {
                    asyncTask.Reject((string)message.GetItem<NSString>(2));
                }
            }
        }

    }
    public async Task<NSObject> EvaluateAsyncJavascriptCode(string script, Func<Task>? afterEval = default)
    {
        /// 页面可能会被刷新，所以需要重新判断：函数可不可用
        var asyncCodeInited = (bool)(NSNumber)await base.EvaluateJavaScriptAsync("typeof " + JS_ASYNC_KIT + "==='object'");
        if (!asyncCodeInited)
        {
            await base.EvaluateJavaScriptAsync(new NSString(asyncCodePrepareCode));
            base.Configuration.UserContentController.AddScriptMessageHandler(asyncCodeMessageHanlder, "asyncCode");
        }
        var id = Interlocked.Increment(ref idAcc);
        var asyncTask = new PromiseOut<NSObject>();
        asyncTaskMap.Add(id, asyncTask);
        var wrapCode = $$"""
            void (async()=>{return ({{script}})})()
                .then(res=>{{JS_ASYNC_KIT}}.resolve({{id}},res))
                .catch(err=>{{JS_ASYNC_KIT}}.reject({{id}},err));
            """;
        await base.EvaluateJavaScriptAsync(wrapCode);

        if (afterEval is not null)
        {
            await afterEval();
        }

        return await asyncTask.WaitPromiseAsync();
    }

}

