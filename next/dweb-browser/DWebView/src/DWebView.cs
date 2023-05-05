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
using System.Reflection;

namespace DwebBrowser.DWebView;

public partial class DWebView : WKWebView
{
    static Debugger Console = new Debugger("DWebView");

    MicroModule localeMM;
    MicroModule remoteMM;
    Options options;

    public class Options
    {
        /// <summary>
        /// 根链接
        /// </summary>
        public Uri? BaseUri;
        /// <summary>
        /// 要加载的页面
        /// </summary>
        public Uri? LoadUrl;

        /// <summary>
        /// 是否对于 .dweb 的根域名，使用 协议来请求，而不是翻译成 http://localhost:port/X-Dweb-Href/https://xxx.dweb/pathname 这样的标准http链接
        /// </summary>
        public bool AllowDwebScheme = true;

        public Signal? OnReady;
        public Options(string url)
        {
            if (Uri.TryCreate(url, UriKind.Absolute, out var uri))
            {
                Init(uri, uri);
            };
        }
        public Options(Uri loadUrl)
        {
            Init(loadUrl, loadUrl);
        }
        public Options() { }
        void Init(Uri? baseUri, Uri? loadUrl)
        {
            this.BaseUri = baseUri;
            this.LoadUrl = loadUrl;
        }
        public static Options Empty = new Options();
    }

    public DWebView(CGRect frame, MicroModule localeMM, MicroModule remoteMM, Options options, WKWebViewConfiguration configuration) : base(frame, configuration.Also((configuration) =>
    {
        if (options.AllowDwebScheme && options.BaseUri is not null and var baseUri)
        {
            TryRegistryUrlSchemeHandler(baseUri, remoteMM, configuration);
        }
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

        /// 绑定 OnReady 监听
        OnReady += options.OnReady;

        /// 加载页面
        if (options.LoadUrl is not null and var loadUrl)
        {
            LoadURL(loadUrl).Background();
        }
        this.UIDelegate = new DWebViewUiDelegate(this);
    }
    public DWebView(CGRect? frame, MicroModule localeMM, MicroModule? remoteMM, Options? options, WKWebViewConfiguration? configuration) : this(frame ?? CGRect.Empty, localeMM, remoteMM ?? localeMM, options ?? Options.Empty, configuration ?? CreateDWebViewConfiguration())
    {
    }

    public DWebView(MicroModule localeMM, MicroModule? remoteMM = default, Options? options = default, CGRect? frame = default, WKWebViewConfiguration? configuration = default) : this(frame ?? CGRect.Empty, localeMM, remoteMM ?? localeMM, options ?? Options.Empty, configuration ?? CreateDWebViewConfiguration())
    {
    }

    public static WKWebViewConfiguration CreateDWebViewConfiguration()
    {
        var configuration = new WKWebViewConfiguration();
        var preferences = configuration.Preferences;
        preferences.JavaScriptCanOpenWindowsAutomatically = true;
        if (OperatingSystem.IsIOSVersionAtLeast(15) || OperatingSystem.IsMacCatalystVersionAtLeast(15))
        {
#pragma warning disable CA1422
            preferences.JavaScriptEnabled = true;
#pragma warning restore CA1422
        }


        var webpagePreferences = configuration.DefaultWebpagePreferences ?? new WKWebpagePreferences();
        webpagePreferences.AllowsContentJavaScript = true;
        configuration.DefaultWebpagePreferences = webpagePreferences;


        configuration.AllowsPictureInPictureMediaPlayback = true;
        configuration.AllowsInlineMediaPlayback = true;
        if (OperatingSystem.IsIOSVersionAtLeast(16) || OperatingSystem.IsMacCatalystVersionAtLeast(15))
        {
#pragma warning disable CA1422
            configuration.MediaPlaybackAllowsAirPlay = true;
#pragma warning restore CA1422
        }
        else
        {
            configuration.AllowsAirPlayForMediaPlayback = true;
        }

        return configuration;

    }

}

