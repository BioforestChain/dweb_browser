package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.microService.browser.BrowserNMM
import info.bagen.rust.plaoc.microService.sys.boot.BootNMM
import info.bagen.rust.plaoc.microService.sys.dns.DnsNMM
import info.bagen.rust.plaoc.microService.sys.http.HttpNMM
import info.bagen.rust.plaoc.microService.sys.jmm.JmmNMM
import info.bagen.rust.plaoc.microService.sys.js.JsProcessNMM
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewNMM
import info.bagen.rust.plaoc.microService.sys.plugin.clipboard.ClipboardNMM
import info.bagen.rust.plaoc.microService.sys.plugin.device.*
import info.bagen.rust.plaoc.microService.sys.plugin.fileSystem.FileSystemNMM
import info.bagen.rust.plaoc.microService.sys.plugin.haptics.HapticsNMM
import info.bagen.rust.plaoc.microService.sys.plugin.notification.NotificationNMM
import info.bagen.rust.plaoc.microService.sys.plugin.permission.PermissionsNMM
import info.bagen.rust.plaoc.microService.sys.plugin.share.ShareNMM
import info.bagen.rust.plaoc.microService.sys.plugin.systemui.KeyboardNMM
import info.bagen.rust.plaoc.microService.sys.plugin.systemui.NavigationBarNMM
import info.bagen.rust.plaoc.microService.sys.plugin.systemui.StatusBarNMM
import info.bagen.rust.plaoc.microService.user.DesktopJMM

suspend fun startDwebBrowser() {
    System.setProperty("dweb-debug", listOf<String>(
//        "message-port-ipc",
//        "stream-ipc",
//        "stream",
    ).joinToString(" ") { it })

    val dnsNMM = DnsNMM()

    /// 安装系统应用
    val jsProcessNMM = JsProcessNMM().also { dnsNMM.install(it) }
    val multiWebViewNMM = MultiWebViewNMM().also { dnsNMM.install(it) }
    val httpNMM = HttpNMM().also { dnsNMM.install(it) }

    /// 安装系统桌面
    val browserNMM = BrowserNMM().also { dnsNMM.install(it) }

    ///安装剪切板
    val clipboardNMM = ClipboardNMM().also { dnsNMM.install(it) }
    ///设备信息
    val deviceNMM = DeviceNMM().also { dnsNMM.install(it) }
    ///位置
    val locationNMM = LocationNMM().also { dnsNMM.install(it) }
    /// 蓝牙
    val bluetoothNMM = BluetoothNMM().also { dnsNMM.install(it) }
    /// 网络
    val networkNMM = NetworkNMM().also { dnsNMM.install(it) }
    ///权限
    val permissionNMM = PermissionsNMM().also { dnsNMM.install(it) }
    ///文件系统
    val fileSystemNMM = FileSystemNMM().also { dnsNMM.install(it) }
    /// NFC
    val nfcNMM = NfcNMM().also { dnsNMM.install(it) }
    /// 通知
    val notificationNMM = NotificationNMM().also { dnsNMM.install(it) }
    /// 分享
    val shareNMM = ShareNMM().also { dnsNMM.install(it) }
    /// 振动效果
    val hapticsNMM = HapticsNMM().also { dnsNMM.install(it) }

    /// keyboard
    val keyboardNMM = KeyboardNMM().also { dnsNMM.install(it) }
    /// statusBar
    val statusBarNMM = StatusBarNMM().also { dnsNMM.install(it) }
    ///navigation
    val navigationBarNMM = NavigationBarNMM().also { dnsNMM.install(it) }


    /// 安装Jmm
    val jmmNMM = JmmNMM().also { dnsNMM.install(it) }

    /// 安装用户应用
    val desktopJMM = DesktopJMM().also { dnsNMM.install(it) }

    /// 启动程序
    val bootNMM = BootNMM(
        listOf(
            browserNMM.mmid,
            desktopJMM.mmid,
        )
    ).also { dnsNMM.install(it) }

    /// 启动
    dnsNMM.bootstrap()
}