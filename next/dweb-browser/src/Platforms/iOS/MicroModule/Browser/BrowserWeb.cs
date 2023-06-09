using WebKit;
using Foundation;
using CoreGraphics;
using System.Runtime.Versioning;
using DwebBrowser.MicroService.Http;
using DwebBrowser.MicroService.Core;
using DwebBrowser.MicroService.Sys.Http;
using ObjCRuntime;
using Microsoft.Maui.Platform;

namespace DwebBrowser.MicroService.Browser;

public partial class BrowserWeb : WKWebView
{
    static Debugger Console = new("BrowserWeb");
    public BrowserWeb(CGRect frame, WKWebViewConfiguration configuration) : base(frame, configuration)
    {
        /// 注入脚本，修改dweb_deeplinks的fetch为window.location.href，否则webview无法拦截到
        var script = new WKUserScript(new NSString($$"""
            var originalFetch = fetch;
            function nativeFetch(input, init) {
              if (input.startsWith('dweb:')) {
                window.location.href = input;
              }
              return originalFetch(input, init);  
            }
            window.fetch = nativeFetch;
        """), WKUserScriptInjectionTime.AtDocumentEnd, true);
        configuration.UserContentController.AddUserScript(script);
        this.NavigationDelegate = new DwebNavigationDelegate();
    }

    public BrowserWeb() : this(CGRect.Empty, new())
    { }
}

sealed class DwebNavigationDelegate : WKNavigationDelegate
{
    [Export("webView:decidePolicyForNavigationAction:decisionHandler:")]
    public override async void DecidePolicy(
        WKWebView webView,
        WKNavigationAction navigationAction,
        Action<WKNavigationActionPolicy> decisionHandler)
    {

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
