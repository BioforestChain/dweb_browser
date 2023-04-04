using Foundation;
using System;
using WebKit;
using UIKit;
using DwebBrowser.Helper;

namespace DwebBrowser.DWebView;

public class DWebViewHelper
{
    WKPreferences preferences;
    WKUserContentController controller;
    WKWebViewConfiguration configuration;

    WKWebView webview;
    public DWebViewHelper()
    {
        this.preferences = new WKPreferences
        {
            JavaScriptCanOpenWindowsAutomatically = true,
            JavaScriptEnabled = true,
        };
        this.controller = new WKUserContentController();
        this.configuration = new WKWebViewConfiguration
        {
            Preferences = this.preferences,
            UserContentController = this.controller,
        };
        this.webview = new WKWebView(CoreGraphics.CGRect.Null, this.configuration);

    }

    public WebMessagePort createWebMessagePort(Object message = null)
    {
        var messagePort = new WebMessagePort();
        return messagePort;
    }

    public class WebMessagePort
    {
        void postMessage(WebMessage message)
        {

        }
        event Signal<WebMessage>? onMessage;

        public class WebMessage
        {
            NSObject data;
            WebMessagePort[] ports;
            internal WebMessage(NSObject data, WebMessagePort[] ports)
            {
                this.data = data;
                this.ports = ports;
            }
        }

    }
}

