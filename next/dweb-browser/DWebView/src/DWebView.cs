using DwebBrowser.Helper;
using DwebBrowser.MicroService.Core;

namespace DwebBrowser.DWebView;

/**
 * DWebView ,将 WebView 与 dweb 的 dwebHttpServer 设计进行兼容性绑定的模块
 * 该对象继承于WebView，所以需要在主线程去初始化它
 *
 * dwebHttpServer 底层提供了三个概念：
 * host/internal_origin/public_origin
 *
 * 其中 public_origin 是指标准的 http 协议链接，可以在标准的网络中被访问（包括本机的其它应用、以及本机所处的局域网），它的本质就是一个网关，所有的本机请求都会由它代理分发。
 * 而 host ，就是所谓网关分发的判定关键
 * 因此 internal_origin 是一个特殊的 http 链接协议，它非标准，只能在本应用（Dweb Browser）被特定的方法翻译后才能正常访问
 * 这个"翻译"方法的背后，本质上就是 host 这个值在其关键作用：
 * 1. 将 host 值放在 url 的 query.X-Dweb-Host。该方法最直接，基于兼容任何环境很高，缺点是用户构建链接的时候，这部分的信息很容易被干扰没掉
 * 2. 将 host 值放在 request 的 header 中 ("X-Dweb-Host: $HOST")。该方法有一定环境要求，需要确保自定义头部能够被设置并传递，缺点在于它很难被广泛地使用，因为自定义 header 就意味着必须基于命令编程而不是声明式的语句
 * 3. 将 host 值放在 url 的 username 中: uri.userInfo(HOST.encodeURI())。该方法相比第一个方法，优点在于不容易被干扰，而且属于声明式语句，可以对链接所处环境做影响。缺点是它属于身份验证的标准，有很多安全性限制，在一些老的API接口中会有奇怪的行为，比如现代浏览器中的iframe是不允许这种url的。
 * 4. 将 host 值 header["Host"] 中: uri.header("Host", HOST)。该方法对环境要求最大，通常用于可编程能力较高环境中，比如 electron 这种浏览器中对 https/http 域名做完全的拦截，或者说 nodejs 这类完全可空的后端环境中对 httpRequest 做完全的自定义构建。这种方案是最标准的存在，但也是最难适配到各个环境的存在。
 *
 * 以上四种方式，优先级依次降低，都可以将 dweb-host 携带给 public_origin 背后的服务让其进行网关路由
 *
 * 再有关于 internal_origin，是一种非标准的概念，它的存在目的是尽可能不要将请求走到 public_origin，因为这会导致我们的数据走了网卡，从而造成应用内数据被窃取，甚至是会被别人使用 http 请求发起恶意攻击。
 * 因此，我们就要在不同平台环境中的，尽可能让这个 internal_origin 标准能广泛地使用。
 * 具体说，在 Dweb-Browser 这个产品中，最大的问题就是浏览器的拦截问题。
 *
 * 当下，Android 想要拦截 POST 等带 body 的请求，必须用 service-worker 来做到，但是 service-worker 本身直接与原生交互，所以在 service-worker 层返回会引入新的问题，最终的结果就是导致性能下降等。同时 Android 的拦截还有一些限制，比如不允许 300～399 的响应等等。
 * IOS 虽然能拦截 body，但是不能像Android一样去拦截 http/https 链接
 * Electron 25 之后，已经能轻松拦截并构建所有的 http/https 请求的响应了
 *
 * 因此 internal_origin 的形态就千奇百怪。
 * 在 Electron 中的开发版使用的是: http://app.gaubee.com.dweb-443.localhost:22600/index.html
 *    未来正式环境版会使用完整版的形态: https://app.gaubee.com.dweb:443/index.html
 * 在 Android 中也是: https://app.gaubee.com.dweb:443/index.html，但只能处理 GET/200|400|500 这类简单的请求，其它情况下还是得使用 public_origin
 * 在 IOS 中使用的是 app.gaubee.com.dweb+443:/index.html 这样的链接
 *
 * 总而言之，如果你的 WebApp 需要很标准复杂的 http 协议的支持，那么只能选择完全使用 public_origin，它走的是标准的网络协议。
 * 否则，可以像 Plaoc 一样，专注于传统前后端分离的 WebApp，那么可以尽可能采用 internal_origin。
 *
 */

public partial class DWebView : WKWebView
{
    static readonly Debugger Console = new("DWebView");

    readonly MicroModule localeMM;
    MicroModule remoteMM;
    readonly Options options;

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
        public static readonly Options Empty = new();
    }

    public DWebView(CGRect frame, MicroModule localeMM, MicroModule remoteMM, Options options, WKWebViewConfiguration configuration) : base(frame, configuration.Also((configuration) =>
    {
        if (options.AllowDwebScheme && options.BaseUri is not null and var baseUri)
        {
            TryRegistryUrlSchemeHandler(baseUri, remoteMM, configuration);
        }

        /// 注入脚本
        AddUserScript(configuration);
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
            _ = LoadURL(loadUrl).NoThrow();
        }
        this.UIDelegate = new DWebViewUIDelegate(this);

        /// 添加JS交互代理
        AddScriptMessageHandler();

        /// 设置 ContentInsetAdjustment 的默认行为，这样 SafeArea 就不会注入到 WKWebView.ScrollView.ContentInset 中
        this.ScrollView.ContentInsetAdjustmentBehavior = UIScrollViewContentInsetAdjustmentBehavior.Never;

        /// 移除关于键盘的默认监听，这样键盘就不会注入到 ScrollView.ContentInset 中
        NSNotificationCenter.DefaultCenter.RemoveObserver(this, UIKeyboard.WillChangeFrameNotification, null);
        NSNotificationCenter.DefaultCenter.RemoveObserver(this, UIKeyboard.WillShowNotification, null);
        NSNotificationCenter.DefaultCenter.RemoveObserver(this, UIKeyboard.WillHideNotification, null);
        /// TODO 键盘的安全区域是可以呗外部控制的，那么就需要实现相关的控制、绑定相关的监听
        this.ScrollView.DidChangeAdjustedContentInset += ScrollView_DidChangeAdjustedContentInset;
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
        // 关闭 window.open 自动打开
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

    private void ScrollView_DidChangeAdjustedContentInset(object? sender, EventArgs e)
    {
        Console.Log("ScrollView_DidChangeAdjustedContentInset", "ContentOffset:{0}", ScrollView.AdjustedContentInset);
        //if (ScrollView.ContentOffset != CGPoint.Empty)
        //{
        //    ScrollView.ContentOffset = CGPoint.Empty;
        //    ScrollView.ContentSize = Frame.Size;
        //}
    }

    private static void AddUserScript(WKWebViewConfiguration configuration)
    {
        try
        {
            configuration.UserContentController.RemoveAllUserScripts();
            var webMessagePortScript = new WKUserScript(
                new NSString(webMessagePortPrepareCode),
                WKUserScriptInjectionTime.AtDocumentEnd,
                false,
                webMessagePortContentWorld);
            configuration.UserContentController.AddUserScript(webMessagePortScript);

            var asyncCodeScript = new WKUserScript(
                new NSString(asyncCodePrepareCode),
                WKUserScriptInjectionTime.AtDocumentEnd,
                false);
            configuration.UserContentController.AddUserScript(asyncCodeScript);
        }
        catch (Exception e)
        {
            Console.Error("AddUserScript", "{0}", e);
        }
    }

    private void AddScriptMessageHandler()
    {
        try
        {
            Configuration.UserContentController.RemoveAllScriptMessageHandlers();
            Configuration.UserContentController.AddScriptMessageHandler(asyncCodeMessageHanlder, "asyncCode");
            Configuration.UserContentController.AddScriptMessageHandler(CloseWatcherMessageHanlder, "closeWatcher");
            Configuration.UserContentController.AddScriptMessageHandler(HapticsMessageHanlder, "haptics");
            Configuration.UserContentController.AddScriptMessageHandler(webMessagePortMessageHanlder, webMessagePortContentWorld, "webMessagePort");
        }
        catch (Exception e)
        {
            Console.Error("_addScriptMessageHandler", "{0}", e);
        }
    }
}

