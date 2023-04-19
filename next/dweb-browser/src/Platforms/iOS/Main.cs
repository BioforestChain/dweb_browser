using System;
using DwebBrowser.MicroService;
using DwebBrowser.MicroService.Core;
using DwebBrowser.Helper;
using DwebBrowser.DWebView;
using DwebBrowser.MicroService.Sys.Dns;
using DwebBrowser.WebModule.Js;
using DwebBrowser.MicroService.Sys.Http;
using DwebBrowser.WebModule.User;
using DwebBrowser.MicroService.Sys.Boot;

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
    static public async Task<DnsNMM> Start()
    {
        var dnsNMM = new DnsNMM();
        /// 安装系统应用
        var jsProcessNMM = new JsProcessNMM().InstallBy(dnsNMM);
        var httpNMM = new HttpNMM().InstallBy(dnsNMM);

        /// 安装用户应用
        var desktopJMM = new DesktopJMM().InstallBy(dnsNMM);


        var bootMmidList = new List<Mmid> { desktopJMM.Mmid };
        /// 启动程序
        var bootNMM = new BootNMM(
            bootMmidList
        ).InstallBy(dnsNMM);

        /// 启动
        await dnsNMM.Bootstrap();

        return dnsNMM;
    }
}
