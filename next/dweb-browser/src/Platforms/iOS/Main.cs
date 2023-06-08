using DwebBrowser.MicroService.User;
using DwebBrowser.MicroService.Sys.Dns;
using DwebBrowser.MicroService.Sys.Http;
using DwebBrowser.MicroService.Sys.Boot;
using DwebBrowser.MicroService.Sys.Share;
using DwebBrowser.MicroService.Sys.Toast;
using DwebBrowser.MicroService.Sys.Haptics;
using DwebBrowser.MicroService.Sys.Barcode;
using DwebBrowser.MicroService.Sys.Clipboard;
using DwebBrowser.MicroService.Sys.Biometrics;
using DwebBrowser.MicroService.Browser;
using DwebBrowser.MicroService.Browser.Jmm;
using DwebBrowser.MicroService.Browser.Mwebview;
using DwebBrowser.MicroService.Browser.NativeUI;
using DwebBrowser.MicroService.Browser.JsProcess;
using DwebBrowser.MicroService.Browser.NativeUI.Torch;

namespace DwebBrowser.Platforms.iOS;

static class MicroModuleExtendions
{
    public static T InstallBy<T>(this T self, DnsNMM dns) where T : MicroModule
    {
        dns.Install(self);
        return self;
    }
}

/// <summary>
/// 全局自治任务异常捕获
/// </summary>
public static class UnobservedTaskExceptionCatch
{
    static Debugger Console = new("UnobservedTaskExceptionCatch");

    public static void Init()
    {
        TaskScheduler.UnobservedTaskException += (sender, e) =>
        {
            Console.Error("Unobserved exception", e.Exception.Message);
        };
    }
}

public class MicroService
{
    #region 日志tag标识
    /// UnobservedTaskExceptionCatch
    /// BiometricsManager
    /// BiometricsNMM
    /// BootNMM
    /// ClipboardNMM
    /// DWebView
    /// HttpNMM
    /// HttpRouter
    /// Ipc
    /// IpcBodyReceiver
    /// IpcBodySender
    /// JmmController
    /// JsMicroModule
    /// JsProcessNMM
    /// JsProcessWebApi
    /// LocaleFile
    /// MessagePort
    /// MessagePortIpc
    /// MultiWebViewController
    /// MultiWebViewNMM
    /// NativePort
    /// NMM
    /// NavigationBarController
    /// NotificationManager
    /// PureRequest
    /// ReadableStreamIpc
    /// ResponseRegistry
    /// SafeAreaController
    /// SafeAreaNMM
    /// ScanningManager
    /// ScanningNMM
    /// Signal
    /// StatusBarController
    /// StatusBarNMM
    /// TorchNMM
    /// VibrateManager
    /// VirtualKeyboardController
    /// VirtualKeyboardNMM
    #endregion

    // 添加debug日志过滤
    private static readonly List<string> _debugTags = new()
    {
        "UnobservedTaskExceptionCatch",
        "JsMicroModule",
        "HttpNMM",
        "LocaleFile",
        "DnsNMM",
        "MessagePortIpc"
    };

    public static async Task<DnsNMM> Start()
    {
        Debugger.DebugTags = _debugTags;
        LocaleFile.Init();
        UnobservedTaskExceptionCatch.Init();

        var dnsNMM = new DnsNMM();
        /// 安装系统应用
        var jsProcessNMM = new JsProcessNMM().InstallBy(dnsNMM);
        var httpNMM = new HttpNMM().InstallBy(dnsNMM);
        var mwebiewNMM = new MultiWebViewNMM().InstallBy(dnsNMM);

        /// 安装系统桌面
        var browserNMM = new BrowserNMM().InstallBy(dnsNMM);

        /// 安装平台模块
        new ShareNMM().InstallBy(dnsNMM);
        new ClipboardNMM().InstallBy(dnsNMM);
        new ToastNMM().InstallBy(dnsNMM);
        new HapticsNMM().InstallBy(dnsNMM);
        new TorchNMM().InstallBy(dnsNMM);
        new ScanningNMM().InstallBy(dnsNMM);
        new BiometricsNMM().InstallBy(dnsNMM);

        /// NativeUi 是将众多原生UI在一个视图中组合的复合组件
        new NativeUiNMM().InstallBy(dnsNMM);

        /// 安装Jmm
        new JmmNMM().InstallBy(dnsNMM);

        /// 安装用户应用
        var desktopJMM = new DesktopJMM().InstallBy(dnsNMM);
        var cotDemoJMM = new CotDemoJMM().InstallBy(dnsNMM);

        var bootMmidList = new List<Mmid>
        {
            //cotDemoJMM.Mmid
            //desktopJMM.Mmid
            browserNMM.Mmid
        };
        /// 启动程序
        var bootNMM = new BootNMM(
            bootMmidList
        ).InstallBy(dnsNMM);

        /// 启动
        await dnsNMM.Bootstrap();

        return dnsNMM;
    }
}
