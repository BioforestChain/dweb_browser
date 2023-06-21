using CoreGraphics;
using Foundation;
using WebKit;

namespace DwebBrowser.MicroService.Browser;

public partial class BrowserWeb : WKWebView
{
    static readonly Debugger Console = new("BrowserWeb");

    private MicroModule _microModule { get; init; }

    //public override NSUrl Url => new("about+newtab://newtab");
    public BrowserWeb(CGRect frame, WKWebViewConfiguration configuration, MicroModule localeMM) : base(
        frame, configuration.Also(configuration =>
        {
            // 注册自定义schema about 用于打开新标签页
            var aboutSchemaHandler = new AboutSchemaHandler(localeMM, new Uri("about+ios://newtab"));
            configuration.SetUrlSchemeHandler(aboutSchemaHandler, aboutSchemaHandler.scheme);

            // http://localhost/browser.dweb/appsInfo
            var browserSchemaHandler = new BrowserSchemaHandler(localeMM, new Uri("Browser.dweb://browser.dweb"));
            configuration.SetUrlSchemeHandler(browserSchemaHandler, "browser.dweb");
            configuration.UserContentController.AddScriptMessageHandler(new ConsoleMessageHandler(), "logging");
        }))
    {
        _microModule = localeMM;

        /// 注入脚本，修改dweb_deeplinks的fetch为window.location.href，否则webview无法拦截到
        var script = new WKUserScript(new NSString($$"""
            var originalFetch = fetch;
            function dwebFetch(input, init) {
                if (input.toString().startsWith === 'dweb:') {
                  window.location.href = input;
                  return;
                } else if(input.toString().startsWith('http://localhost/browser.dweb/')) {
                  input = new URL(input.toString().replace('http://localhost/', 'browser.dweb://'));
                  window.webkit.messageHandlers.logging.postMessage({log:'dwebFetch1: ' + input});
                }
                return originalFetch(input, init); 
            }
            window.fetch = dwebFetch;
        """), WKUserScriptInjectionTime.AtDocumentStart, false);
        configuration.UserContentController.AddUserScript(script);

        // 拦截dweb-deepLinks
        NavigationDelegate = new DwebNavigationDelegate();
        //UIDelegate = new BrowserUiDelegate(localeMM);
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
            return CreateDWebView(navigationAction.Request.Url.AbsoluteString);
        }
    }

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

    class BrowserSchemaHandler : DWebView.DWebView.DwebSchemeHandler
    {
        public BrowserSchemaHandler(MicroModule microModule, Uri url) : base(microModule, url)
        {
            scheme = url.Scheme;
        }

        public override Uri ResetSchemeUrl(NSUrl nsurl)
        {
            var url = nsurl.AbsoluteString.Replace("browser.dweb://", "file://");

            return new(url);
        }
    }

    class AboutSchemaHandler : DWebView.DWebView.DwebSchemeHandler
    {
        public AboutSchemaHandler(MicroModule microModule, Uri url) : base(microModule, url)
        {
            scheme = url.Scheme;
        }

        public override Uri ResetSchemeUrl(NSUrl nsurl)
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
}


