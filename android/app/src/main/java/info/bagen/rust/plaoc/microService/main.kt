package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.microService.sys.boot.BootNMM
import info.bagen.rust.plaoc.microService.sys.dns.DnsNMM
import info.bagen.rust.plaoc.microService.sys.http.HttpNMM
import info.bagen.rust.plaoc.microService.sys.js.JsProcessNMM
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewNMM
import info.bagen.rust.plaoc.microService.user.DesktopJMM

suspend fun startDwebBrowser() {
    val dnsNMM = DnsNMM()

    /// 安装系统应用
    dnsNMM.install(JsProcessNMM())
    dnsNMM.install(BootNMM())
    dnsNMM.install(MultiWebViewNMM())
    dnsNMM.install(HttpNMM())

    /// 安装用户应用
    dnsNMM.install(DesktopJMM())

    /// 启动
    dnsNMM.bootstrap()
}