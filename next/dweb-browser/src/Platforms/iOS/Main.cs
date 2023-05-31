using System;
using DwebBrowser.MicroService;
using DwebBrowser.MicroService.Core;
using DwebBrowser.Helper;
using DwebBrowser.DWebView;
using DwebBrowser.MicroService.Sys.Dns;
using DwebBrowser.MicroService.Sys.Js;
using DwebBrowser.MicroService.Sys.Http;
using DwebBrowser.MicroService.Sys.User;
using DwebBrowser.MicroService.Sys.Boot;
using DwebBrowser.MicroService.Sys.Mwebview;
using DwebBrowser.MicroService.Sys.NativeUI;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.Torch;
using DwebBrowser.Platforms.iOS.MicroModule.Plugin.Share;
using DwebBrowser.Platforms.iOS.MicroModule.Plugin.Clipboard;
using DwebBrowser.Platforms.iOS.MicroModule.Plugin.Toast;
using DwebBrowser.Platforms.iOS.MicroModule.Plugin.Haptics;
using DwebBrowser.Platforms.iOS.MicroModule.Plugin.Barcode;
using DwebBrowser.Platforms.iOS.MicroModule.Plugin.Biometrics;

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
    public static async Task<DnsNMM> Start()
    {
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
