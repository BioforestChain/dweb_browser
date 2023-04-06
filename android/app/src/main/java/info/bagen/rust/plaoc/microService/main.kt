package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.microService.browser.BrowserNMM
import info.bagen.rust.plaoc.microService.helper.debugTags
import info.bagen.rust.plaoc.microService.sys.boot.BootNMM
import info.bagen.rust.plaoc.microService.sys.dns.DnsNMM
import info.bagen.rust.plaoc.microService.sys.http.HttpNMM
import info.bagen.rust.plaoc.microService.sys.jmm.JmmNMM
import info.bagen.rust.plaoc.microService.sys.js.JsProcessNMM
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewNMM
import info.bagen.rust.plaoc.microService.sys.nativeui.NativeUiNMM
import info.bagen.rust.plaoc.microService.sys.nativeui.torch.TorchNMM
import info.bagen.rust.plaoc.microService.sys.plugin.barcode.ScanningNMM
import info.bagen.rust.plaoc.microService.sys.plugin.camera.CameraNMM
import info.bagen.rust.plaoc.microService.sys.plugin.clipboard.ClipboardNMM
import info.bagen.rust.plaoc.microService.sys.plugin.device.*
import info.bagen.rust.plaoc.microService.sys.plugin.fileSystem.FileSystemNMM
import info.bagen.rust.plaoc.microService.sys.plugin.haptics.HapticsNMM
import info.bagen.rust.plaoc.microService.sys.plugin.notification.NotificationNMM
import info.bagen.rust.plaoc.microService.sys.plugin.permission.PermissionsNMM
import info.bagen.rust.plaoc.microService.sys.plugin.share.ShareNMM
import info.bagen.rust.plaoc.microService.sys.plugin.toast.ToastNMM
import info.bagen.rust.plaoc.microService.user.CotDemoJMM
import info.bagen.rust.plaoc.microService.user.CotJMM
import info.bagen.rust.plaoc.microService.user.DesktopJMM
import info.bagen.rust.plaoc.microService.user.ToyJMM

suspend fun startDwebBrowser(): DnsNMM {
    /**
    "message-port-ipc",
    "stream-ipc",
    "stream",
    "fetch",
    "fetch-file",
    "ipc-body",
    "http",
    "TIME-DURATION"
    "nativeui",
    "mwebview",
    "browser",
    "jmm",
    "SplashScreen",
     "DwebServiceWorker"
     */
    when (DEVELOPER.CURRENT) {
        DEVELOPER.GAUBEE -> debugTags.addAll(
            listOf<String>(
                "message-port-ipc",
                "stream-ipc",
                "stream",
                "ipc-body",
                "dwebview",
            )
        )
        DEVELOPER.HuangLin, DEVELOPER.HLVirtual -> debugTags.addAll(
            listOf("Share", "fetch", "http", "jmm", "browser")
        )
        DEVELOPER.WaterBang -> debugTags.addAll(
            listOf("Share","DwebServiceWorker","http")
        )
        else -> debugTags.addAll(
            listOf("Share","FileSystem")
        )
    }

    val dnsNMM = DnsNMM()

    /// 安装系统应用
    val jsProcessNMM = JsProcessNMM().also { dnsNMM.install(it) }
    val multiWebViewNMM = MultiWebViewNMM().also { dnsNMM.install(it) }
    val httpNMM = HttpNMM().also { dnsNMM.install(it) }

    /// 安装系统桌面
    val browserNMM = BrowserNMM().also { dnsNMM.install(it) }

    /// 相机
    val cameraNMM = CameraNMM().also { dnsNMM.install(it) }
    /// 扫码
    val scannerNMM = ScanningNMM().also { dnsNMM.install(it) }
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
    /// 弹窗
    val toastNMM = ToastNMM().also { dnsNMM.install(it) }
    /// 分享
    val shareNMM = ShareNMM().also { dnsNMM.install(it) }
    /// 振动效果
    val hapticsNMM = HapticsNMM().also { dnsNMM.install(it) }
    /// 手电筒
    val torchNMM = TorchNMM().also() { dnsNMM.install(it) }

    /// NativeUi 是将众多原生UI在一个视图中组合的复合组件
    NativeUiNMM().also { dnsNMM.install(it) }

    /// 安装Jmm
    val jmmNMM = JmmNMM().also { dnsNMM.install(it) }

    /// 安装用户应用
    val desktopJMM = DesktopJMM().also { dnsNMM.install(it) }
    val cotJMM = CotJMM().also { dnsNMM.install(it) }
    val cotDemoJMM = CotDemoJMM().also { dnsNMM.install(it) }
    val toyJMM = ToyJMM().also { dnsNMM.install(it) }

    /**
     *
     * browserNMM.mmid,
     * desktopJMM.mmid,
     * cotDemoJMM.mmid,
     * cotJMM.mmid,
     * toyJMM.mmid,
     */
    val bootMmidList = when (DEVELOPER.CURRENT) {
        DEVELOPER.GAUBEE -> listOf(
//            cotJMM.mmid,
//            cotDemoJMM.mmid,
            browserNMM.mmid,
        )
        DEVELOPER.HuangLin, DEVELOPER.HLVirtual -> listOf(browserNMM.mmid)
        DEVELOPER.WaterBang -> listOf(cotDemoJMM.mmid,browserNMM.mmid)
        else -> {
            listOf(
                browserNMM.mmid,
            )
        }
    }

    /// 启动程序
    val bootNMM = BootNMM(
        bootMmidList
    ).also { dnsNMM.install(it) }

    /// 启动
    dnsNMM.bootstrap()
    return dnsNMM
}
