using DwebBrowser.Helper;
using DwebBrowser.MicroService.Core;
using WebKit;

namespace DwebBrowser.DWebView;

public partial class DWebView : WKWebView
{
    static readonly Debugger Console = new("DWebView");

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

