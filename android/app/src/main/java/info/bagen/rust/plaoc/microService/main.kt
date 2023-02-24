package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.microService.sys.base.BrowserNMM
import info.bagen.rust.plaoc.microService.sys.boot.BootNMM
import info.bagen.rust.plaoc.microService.sys.dns.DnsNMM
import info.bagen.rust.plaoc.microService.sys.http.HttpNMM
import info.bagen.rust.plaoc.microService.sys.js.JsProcessNMM
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewNMM
import info.bagen.rust.plaoc.microService.user.DesktopJMM

suspend fun startDwebBrowser() {
    val dnsNMM = DnsNMM()

    /// 安装系统应用
    val jsProcessNMM = JsProcessNMM().also { dnsNMM.install(it) }
    val multiWebViewNMM = MultiWebViewNMM().also { dnsNMM.install(it) }
    val httpNMM = HttpNMM().also { dnsNMM.install(it) }

    /// 安装系统桌面
    val browserNMM = BrowserNMM().also { dnsNMM.install(it) }

    /// 安装用户应用
    val desktopJMM = DesktopJMM().also { dnsNMM.install(it) }

    /// 启动程序
    val bootNMM = BootNMM(listOf(desktopJMM.mmid, browserNMM.mmid)).also { dnsNMM.install(it) }

    /// 启动
    dnsNMM.bootstrap()
}