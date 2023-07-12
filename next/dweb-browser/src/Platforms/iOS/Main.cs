using DwebBrowser.MicroService.Browser;
using DwebBrowser.MicroService.Browser.Jmm;
using DwebBrowser.MicroService.Browser.JsProcess;
using DwebBrowser.MicroService.Browser.Mwebview;
using DwebBrowser.MicroService.Browser.NativeUI;
using DwebBrowser.MicroService.Browser.NativeUI.Haptics;
using DwebBrowser.MicroService.Browser.NativeUI.Torch;
using DwebBrowser.MicroService.Sys.Barcode;
using DwebBrowser.MicroService.Sys.Biometrics;
using DwebBrowser.MicroService.Sys.Boot;
using DwebBrowser.MicroService.Sys.Clipboard;
using DwebBrowser.MicroService.Sys.Device;
using DwebBrowser.MicroService.Sys.Dns;
using DwebBrowser.MicroService.Sys.Haptics;
using DwebBrowser.MicroService.Sys.Http;
using DwebBrowser.MicroService.Sys.Share;
using DwebBrowser.MicroService.Sys.Toast;
using DwebBrowser.MicroService.Test;

namespace DwebBrowser.Platforms.iOS;

static class MicroModuleExtendions
{
    public static T InstallBy<T>(this T self, DnsNMM dns) where T : MicroModule
    {
        dns.Install(self);
        return self;
    }
}

public class MicroService
{
    #region 日志scope标识
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
    /// JmmNMM
    /// StreamExtensions
    /// MicroModule
    #endregion

    // 添加debug日志scope过滤
    private static readonly List<string> _debugScopes = new()
    {
        "JsMicroModule",
        "HttpNMM",
        "LocaleFile",
        "DnsNMM",
        "MessagePortIpc",
        "JmmNMM",
        "BrowserWeb",
        "JmmDownload",
        "StreamExtensions",
        "BrowserNMM",
        "BrowserWeb",
        "DeviceNMM",
        "DeviceSystemInfo"
    };

    // 添加debug日志tag过滤
    private static readonly List<string> _debugTags = new()
    {
        "*"
    };

    private static void _loggerInit()
    {
        Debugger.DebugScopes = _debugScopes;
        Debugger.DebugTags = _debugTags;
    }

    public static async Task<DnsNMM> Start()
    {
        _loggerInit();
        LocaleFile.Init();

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
        new HapticsBrowserNMM().InstallBy(dnsNMM);
        new TorchNMM().InstallBy(dnsNMM);
        new ScanningNMM().InstallBy(dnsNMM);
        new BiometricsNMM().InstallBy(dnsNMM);
        new DeviceNMM().InstallBy(dnsNMM);

        /// NativeUi 是将众多原生UI在一个视图中组合的复合组件
        new NativeUiNMM().InstallBy(dnsNMM);

        /// 安装Jmm
        new JmmNMM().InstallBy(dnsNMM);

        /// 安装测试应用
        var plaocDemoJMM = new PlaocDemoJMM().InstallBy(dnsNMM);

        var bootMmidList = new List<Mmid>
        {
            browserNMM.Mmid,
            plaocDemoJMM.Mmid
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
