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



    public DWebView(CGRect frame, MicroModule localeMM, MicroModule remoteMM, Options options, WKWebViewConfiguration configuration) : base(frame, configuration.Also(configuration =>
    {
        //configuration.SetUrlSchemeHandler(new DwebSchemeHandler(remoteMM), urlScheme: "dweb");
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
        this.UIDelegate = new DWebViewUiDelegate(this);
    }
    public DWebView(CGRect? frame, MicroModule localeMM, MicroModule? remoteMM, Options? options, WKWebViewConfiguration? configuration) : this(frame ?? CGRect.Empty, localeMM, remoteMM ?? localeMM, options ?? Options.Empty, configuration ?? CreateDWebViewConfiguration())
    {
    }

    public DWebView(MicroModule localeMM, MicroModule? remoteMM = default, Options? options = default, CGRect? frame = default, WKWebViewConfiguration? configuration = default) : this(frame ?? CGRect.Empty, localeMM, remoteMM ?? localeMM, options ?? Options.Empty, configuration ?? CreateDWebViewConfiguration())
    {
    }


}

