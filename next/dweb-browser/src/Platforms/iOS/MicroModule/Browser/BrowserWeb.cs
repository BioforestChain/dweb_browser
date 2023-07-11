using System.Runtime.Versioning;
using CoreGraphics;
using DwebBrowser.MicroService.Http;
using Foundation;
using UIKit;
using WebKit;
using DwebBrowserFramework;

namespace DwebBrowser.MicroService.Browser;

public partial class BrowserWeb : BrowserWebview
{
    static readonly Debugger Console = new("BrowserWeb");

    public BrowserWeb(CGRect frame, WKWebViewConfiguration configuration, MicroModule localeMM) : base(
        frame, configuration.Also(configuration =>
        {
            // 关闭自动播放功能
            configuration.MediaTypesRequiringUserActionForPlayback = WKAudiovisualMediaTypes.All;

            // 注册自定义schema about 用于打开新标签页
            var aboutSchemaHandler = new AboutSchemaHandler(localeMM, new Uri("about+ios://newtab"));
            configuration.SetUrlSchemeHandler(aboutSchemaHandler, aboutSchemaHandler.scheme);
            //configuration.UserContentController.AddScriptMessageHandler(new ConsoleMessageHandler(), "logging");
        }))
    {
        /// 注入脚本，修改dweb_deeplinks的fetch为window.location.href，否则webview无法拦截到
        var script = new WKUserScript(new NSString($$"""
            var originalFetch = fetch;
            function dwebFetch(input, init) {
                if (input.toString().startsWith === 'dweb:') {
                  window.location.href = input;
                  return;
                }

                return originalFetch(input, init);
            }
            window.fetch = dwebFetch;
        """), WKUserScriptInjectionTime.AtDocumentStart, false);
        Configuration.UserContentController.AddUserScript(script);

        // 拦截dweb-deepLinks
        NavigationDelegate = new DwebNavigationDelegate();
        UIDelegate = new BrowserUiDelegate(localeMM);
    }

    public BrowserWeb(MicroModule localeMM, WKWebViewConfiguration? configuration = null) : this(
        CGRect.Empty, configuration ?? DWebView.DWebView.CreateDWebViewConfiguration(), localeMM)
    { }

    public BrowserWeb(MicroModule localeMM) : this(localeMM, null)
    { }

    public override WKNavigation LoadRequest(NSUrlRequest request)
    {
        if (request.Url.AbsoluteString == "about:newtab")
        {
            // 如果about+ios:后面这个双斜线不添加的话，html页面中的脚本和样式不会发起请求，无法获取完整页面
            var _request = new NSUrlRequest(new NSUrl("about+ios://newtab"), request.CachePolicy, request.TimeoutInterval);
            return LoadRequest(_request);
        }

        return base.LoadRequest(request);
    }

    sealed class BrowserUiDelegate : WKUIDelegate
    {
        private MicroModule _microModule { get; init; }

        public BrowserUiDelegate(MicroModule microModule)
        {
            _microModule = microModule;
        }

        public async Task<DWebView.DWebView> CreateDWebViewAsync(string url) =>
            new DWebView.DWebView(_microModule, options: new DWebView.DWebView.Options(url));

        public DWebView.DWebView CreateDWebView(string url, WKWebViewConfiguration? configuration = null) =>
            new(_microModule, options: new DWebView.DWebView.Options(url), configuration: configuration);


        [Export("webView:createWebViewWithConfiguration:forNavigationAction:windowFeatures:")]
        public override WKWebView? CreateWebView(WKWebView webView, WKWebViewConfiguration configuration, WKNavigationAction navigationAction, WKWindowFeatures windowFeatures)
        {
            return CreateDWebView("", configuration);
        }
    }

    #region dweb-deeplinks 拦截
    sealed class DwebNavigationDelegate : WKNavigationDelegate
    {
        [Export("webView:decidePolicyForNavigationAction:decisionHandler:")]
        public override async void DecidePolicy(
            WKWebView webView,
            WKNavigationAction navigationAction,
            Action<WKNavigationActionPolicy> decisionHandler)
        {
            Console.Log("DecidePolicy", "url: {0}", navigationAction.Request.Url);
            if (navigationAction.Request.Url.Scheme == "dweb")
            {
                await BrowserNMM.BrowserController?.BrowserNMM.NativeFetchAsync(
                        navigationAction.Request.Url.AbsoluteString);

                decisionHandler(WKNavigationActionPolicy.Cancel);
                return;
            }

            decisionHandler(WKNavigationActionPolicy.Allow);
        }

        /// 3D Touch功能控制
        public override void DidFinishNavigation(WKWebView webView, WKNavigation navigation)
        {
            Console.Log("DidFinishNavigation", webView.Url.AbsoluteString);
            if (webView.Url.Scheme is "about" or "about+ios")
            {
                webView.InvokeOnMainThread(async () =>
                {
                    await webView.EvaluateJavaScriptAsync("""
                        document.documentElement.style.webkitTouchCallout = 'none';
                        """);
                });
            }
            else
            {
                webView.InvokeOnMainThread(async () =>
                {
                    await webView.EvaluateJavaScriptAsync("""
                        document.documentElement.style.webkitTouchCallout = 'default';
                        """);
                });
            }
        }

        /// 因为C#也实现了WKNavigationDelegate，必须执行一次 watchIosIcon ，才能触发获取favicon的行为
        /// 否则iOS tab无法触发favicon获取
        //public override void DidCommitNavigation(WKWebView webView, WKNavigation navigation)
        //{
        //    webView.InvokeOnMainThread(async () =>
        //    {
        //        await webView.EvaluateJavaScriptAsync("""
        //                void watchIosIcon();
        //                """);
        //    });
        //}
    }
    #endregion

    class ConsoleMessageHandler : WKScriptMessageHandler
    {
        [Export("userContentController:didReceiveScriptMessage:")]
        public override async void DidReceiveScriptMessage(WKUserContentController userContentController, WKScriptMessage messageEvent)
        {
            var message = messageEvent.Body;
            var log = (string)(NSString)message.ValueForKey(new NSString("log"));
            Console.Log("DidReceiveScriptMessage", log);
        }
    }

    #region about: 协议拦截
    class AboutSchemaHandler : DWebView.DWebView.DwebSchemeHandler
    {
        private readonly Dictionary<string, string> _corsHeaders = new()
        {
            { "Access-Control-Allow-Origin", "*" },
            { "Access-Control-Allow-Headers", "*" },
            { "Access-Control-Allow-Methods", "*" }
        };

        public AboutSchemaHandler(MicroModule microModule, Uri url) : base(microModule, url)
        {
            scheme = url.Scheme;
        }

        public override Uri InternalSchemaUrl(NSUrl nsurl)
        {
            var url = new URL(nsurl.AbsoluteString.Replace("about+ios://", "http://browser.dweb/"));
            var paths = url.Path.Split("/").ToList().FindAll(it => !string.IsNullOrEmpty(it));

            if (paths[0] == "newtab")
            {
                paths = paths.Skip(1).ToList();

                if (paths.Count > 0 && paths[0] == "api")
                {
                    var _url = string.Format("file://{0}?{1}", string.Join("/", paths.Skip(1)), nsurl.Query);
                    return new(_url);
                }
                else
                {
                    return new(string.Format("file:///sys/browser/newtab/{0}",
                        paths.Count == 0 ? "index.html" : string.Join("/", paths)));
                }
            }
            else
            {
                return new("");
            }
        }

        [Export("webView:startURLSchemeTask:")]
        [SupportedOSPlatform("ios11.0")]
        public override async void StartUrlSchemeTask(WKWebView webView, IWKUrlSchemeTask urlSchemeTask)
        {
            Console.Log("StartUrlSchemeTask", "Start: {0}", urlSchemeTask.Request.Url.AbsoluteString);
            var url = InternalSchemaUrl(urlSchemeTask.Request.Url);
            try
            {
                /// 构建请求
                var pureRequest = new PureRequest(
                    url.AbsoluteUri,
                    IpcMethod.From(urlSchemeTask.Request.HttpMethod),
                    /// 构建请求的 Headers
                    urlSchemeTask.Request.Headers.Select((kv) =>
                    {
                        return KeyValuePair.Create((string)(NSString)kv.Key, (string)(NSString)kv.Value);
                    }).ToIpcHeaders(),
                    /// 构建请求的 ContentBody
                    urlSchemeTask.Request.BodyStream switch
                    {
                        null => null,
                        var nsBodyStream => new PureStreamBody(new NSStream(nsBodyStream))
                    });

                /// 获得响应
                var pureResponse = await microModule.NativeFetchAsync(pureRequest);
                /// 获得响应的状态码
                var nsStatusCode = new IntPtr((int)pureResponse.StatusCode);
                /// 构建响应的头部
                var nsHeaders = new NSMutableDictionary<NSString, NSString>();
                foreach (var (key, value) in pureResponse.Headers)
                {
                    nsHeaders.Add(new NSString(key), new NSString(value));
                }

                if (!string.IsNullOrEmpty(url.Host))
                {
                    /// 添加跨域头，否则fetch会失败
                    foreach (var (key, value) in _corsHeaders)
                    {
                        nsHeaders.Add(new NSString(key), new NSString(value));
                    }
                }

                using var nsResponse = new NSHttpUrlResponse(urlSchemeTask.Request.Url, nsStatusCode, "HTTP/1.1", nsHeaders);

                /// 写入响应头
                urlSchemeTask.DidReceiveResponse(nsResponse);

                // 写入响应体：将响应体发送到urlSchemeTask
                switch (pureResponse.Body)
                {
                    case PureEmptyBody: break;
                    case PureStreamBody streamBody:
                        await foreach (var chunk in streamBody.Data.ReadBytesStream())
                        {
                            urlSchemeTask.DidReceiveData(NSData.FromArray(chunk));
                        }
                        break;
                    case IPureBody body:
                        var data = body.ToByteArray();
                        if (data.Length > 0)
                        {
                            urlSchemeTask.DidReceiveData(NSData.FromArray(data));
                        }
                        break;
                };


                /// 写入完成
                urlSchemeTask.DidFinish();

                Console.Log("StartUrlSchemeTask", "End: {0}", urlSchemeTask.Request.Url.AbsoluteString);
            }
            catch (Exception err)
            {
                if (err is ObjCRuntime.ObjCException cerr)
                {
                    /// 
                    if (cerr.Name == "NSInternalInconsistencyException" && cerr.Reason == "This task has already been stopped")
                    {
                        Console.Warn("StartUrlSchemeTask", "Request Aborted: {0}", urlSchemeTask.Request.Url.AbsoluteString);
                        return;
                    }
                }
                using var nsResponse = new NSHttpUrlResponse(baseUri, new IntPtr(502), "HTTP/1.1", new());
                urlSchemeTask.DidReceiveResponse(nsResponse);
                urlSchemeTask.DidReceiveData(NSData.FromString(err.Message));
                urlSchemeTask.DidFinish();
            }
        }
    }
    #endregion
}


