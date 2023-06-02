using DwebBrowser.MicroService.Sys.Dns;
using DwebBrowser.MicroService.Sys.Http;
using DwebBrowser.MicroService.Sys.Boot;
using DwebBrowser.MicroService.Sys.Share;
using DwebBrowser.MicroService.Sys.Clipboard;
using DwebBrowser.MicroService.Sys.Toast;
using DwebBrowser.MicroService.Sys.Haptics;
using DwebBrowser.MicroService.Sys.Barcode;
using DwebBrowser.MicroService.Sys.Biometrics;
using DwebBrowser.MicroService.Browser.Js;
using DwebBrowser.MicroService.Browser.Mwebview;
using DwebBrowser.MicroService.Browser.NativeUI;
using DwebBrowser.MicroService.Browser.NativeUI.Torch;
using DwebBrowser.MicroService.User;

namespace DwebBrowser.Platforms.iOS;

static class MicroModuleExtendions
{
    public static T InstallBy<T>(this T self, DnsNMM dns) where T : DwebBrowser.MicroService.Core.MicroModule
    {
        dns.Install(self);
        return self;
    }
}
public class MicroService
{
    #region 日志tag标识
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
        var dnsNMM = new DnsNMM();
        /// 安装系统应用
        var jsProcessNMM = new JsProcessNMM().InstallBy(dnsNMM);
        var httpNMM = new HttpNMM().InstallBy(dnsNMM);
        var mwebiewNMM = new MultiWebViewNMM().InstallBy(dnsNMM);

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

        /// 安装用户应用
        var desktopJMM = new DesktopJMM().InstallBy(dnsNMM);
        var cotDemoJMM = new CotDemoJMM().InstallBy(dnsNMM);

        var bootMmidList = new List<Mmid>
        {
            cotDemoJMM.Mmid
            //desktopJMM.Mmid
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
