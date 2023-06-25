using System.Runtime.Versioning;
using CoreGraphics;
using DwebBrowser.DWebView;
using DwebBrowser.Helper;
using DwebBrowser.MicroService.Http;
using Foundation;
using UIKit;
using WebKit;

namespace DwebBrowser.MicroService.Browser;

public partial class BrowserWeb : WKWebView
{
    static readonly Debugger Console = new("BrowserWeb");

    public BrowserWeb(CGRect frame, WKWebViewConfiguration configuration, MicroModule localeMM) : base(
        frame, configuration.Also(configuration =>
        {
            // 注册自定义schema about 用于打开新标签页
            var aboutSchemaHandler = new AboutSchemaHandler(localeMM, new Uri("about+ios://newtab"));
            configuration.SetUrlSchemeHandler(aboutSchemaHandler, aboutSchemaHandler.scheme);

            // http://localhost/browser.dweb/appsInfo
            var browserSchemaHandler = new BrowserSchemaHandler(localeMM, new Uri("browser.dweb://browser.dweb"));
            configuration.SetUrlSchemeHandler(browserSchemaHandler, "browser.dweb");
            configuration.UserContentController.AddScriptMessageHandler(new ConsoleMessageHandler(), "logging");
        }))
    {
        /// 注入脚本，修改dweb_deeplinks的fetch为window.location.href，否则webview无法拦截到
        var script = new WKUserScript(new NSString($$"""
            var originalFetch = fetch;
            function dwebFetch(input, init) {
                if (input.toString().startsWith === 'dweb:') {
                  window.location.href = input;
                  return;
                } else if(input.toString().startsWith('http://localhost/browser.dweb/')) {
                  input = new URL(input.toString().replace('http://localhost/', 'browser.dweb://'));
                  //window.webkit.messageHandlers.logging.postMessage({log:'dwebFetch1: ' + input});
                }

                return originalFetch(input, init);
            }
            window.fetch = dwebFetch;
        """), WKUserScriptInjectionTime.AtDocumentStart, false);
        configuration.UserContentController.AddUserScript(script);

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
        if (request.Url.AbsoluteString == "about://newtab")
        {
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

        public DWebView.DWebView CreateDWebView(string url) =>
            new DWebView.DWebView(_microModule, options: new DWebView.DWebView.Options(url));


        [Export("webView:createWebViewWithConfiguration:forNavigationAction:windowFeatures:")]
        public override WKWebView? CreateWebView(WKWebView webView, WKWebViewConfiguration configuration, WKNavigationAction navigationAction, WKWindowFeatures windowFeatures)
        {
            var webview = CreateDWebView(navigationAction.Request.Url.AbsoluteString);
            return webview;
        }

        //[Export("webView:runJavaScriptAlertPanelWithMessage:initiatedByFrame:completionHandler:")]
        //public override async void RunJavaScriptAlertPanel(WKWebView webView, string message, WKFrameInfo frame, Action completionHandler)
        //{
        //    var alertController = UIAlertController.Create(webView.Title, message, UIAlertControllerStyle.Alert);
        //    /// 点击确定
        //    alertController.AddAction(UIAlertAction.Create("Ok", UIAlertActionStyle.Default, (action) => Console.Log("alert", "1")));

        //    var vc = await IOSNativeMicroModule.RootViewController.WaitPromiseAsync();
        //    await vc.PresentViewControllerAsync(alertController, true);

        //    completionHandler();
        //}

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

    #region browser.dweb:// 协议拦截
    class BrowserSchemaHandler : DWebView.DWebView.DwebSchemeHandler
    {
        private Dictionary<string, string> _corsHeaders = new()
        {
            { "Access-Control-Allow-Origin", "*" },
            { "Access-Control-Allow-Headers", "*" },
            { "Access-Control-Allow-Methods", "*" }
        };

        public BrowserSchemaHandler(MicroModule microModule, Uri url) : base(microModule, url)
        {
            scheme = url.Scheme;
        }

        public override Uri InternalSchemaUrl(NSUrl nsurl)
        {
            var url = nsurl.AbsoluteString.Replace("browser.dweb://", "file://");

            return new(url);
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

                /// 添加跨域头，否则fetch会失败
                foreach (var (key, value) in _corsHeaders)
                {
                    nsHeaders.Add(new NSString(key), new NSString(value));
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
                    case PureBody body:
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

    #region about:// 协议拦截
    class AboutSchemaHandler : DWebView.DWebView.DwebSchemeHandler
    {
        public AboutSchemaHandler(MicroModule microModule, Uri url) : base(microModule, url)
        {
            scheme = url.Scheme;
        }

        public override Uri InternalSchemaUrl(NSUrl nsurl)
        {
            var url = nsurl.AbsoluteString;
            if (url == "about+ios://newtab" || url == "about+ios://newtab/")
            {
                url = "about+ios://newtab/index.html";
            }

            if (url.StartsWith("about+ios://newtab"))
            {
                url = url.Replace("about+ios://newtab", "file:///sys/browser/newtab");
            }
            return new(url);
        }
    }
    #endregion
}


