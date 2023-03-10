package info.bagen.rust.plaoc.microService

import android.os.Build
import info.bagen.rust.plaoc.microService.browser.BrowserNMM
import info.bagen.rust.plaoc.microService.helper.debugTags
import info.bagen.rust.plaoc.microService.sys.boot.BootNMM
import info.bagen.rust.plaoc.microService.sys.dns.DnsNMM
import info.bagen.rust.plaoc.microService.sys.http.HttpNMM
import info.bagen.rust.plaoc.microService.sys.jmm.JmmNMM
import info.bagen.rust.plaoc.microService.sys.js.JsProcessNMM
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewNMM
import info.bagen.rust.plaoc.microService.sys.plugin.barcode.ScanningNMM
import info.bagen.rust.plaoc.microService.sys.plugin.camera.TorchNMM
import info.bagen.rust.plaoc.microService.sys.plugin.clipboard.ClipboardNMM
import info.bagen.rust.plaoc.microService.sys.plugin.device.*
import info.bagen.rust.plaoc.microService.sys.plugin.fileSystem.FileSystemNMM
import info.bagen.rust.plaoc.microService.sys.plugin.haptics.HapticsNMM
import info.bagen.rust.plaoc.microService.sys.plugin.notification.NotificationNMM
import info.bagen.rust.plaoc.microService.sys.plugin.permission.PermissionsNMM
import info.bagen.rust.plaoc.microService.sys.plugin.share.ShareNMM
import info.bagen.rust.plaoc.microService.sys.plugin.splash.SplashScreenNMM
import info.bagen.rust.plaoc.microService.sys.plugin.systemui.KeyboardNMM
import info.bagen.rust.plaoc.microService.sys.plugin.systemui.NavigationBarNMM
import info.bagen.rust.plaoc.microService.sys.plugin.systemui.StatusBarNMM
import info.bagen.rust.plaoc.microService.sys.plugin.toast.ToastNMM
import info.bagen.rust.plaoc.microService.user.CotDemoJMM
import info.bagen.rust.plaoc.microService.user.CotJMM
import info.bagen.rust.plaoc.microService.user.DesktopJMM
import info.bagen.rust.plaoc.microService.user.ToyJMM


suspend fun startDwebBrowser() {
    /**
     *
     * "message-port-ipc",
     * "stream-ipc",
     * "fetch",
     * "stream",
     * "ipc-body",
     * "http",
     * "TIME-DURATION"
     */
    when (DEVELOPER.CURRENT) {
        DEVELOPER.GAUBEE -> debugTags.addAll(
            listOf<String>(
                "mwebview",
                "dwebview",
            )
        )
        else -> debugTags.addAll(
            listOf<String>(

            )
        )
    }


    val dnsNMM = DnsNMM()

    /// ??????????????????
    val jsProcessNMM = JsProcessNMM().also { dnsNMM.install(it) }
    val multiWebViewNMM = MultiWebViewNMM().also { dnsNMM.install(it) }
    val httpNMM = HttpNMM().also { dnsNMM.install(it) }

    /// ??????????????????
    val browserNMM = BrowserNMM().also { dnsNMM.install(it) }

    /// ??????
    val scannerNMM = ScanningNMM().also { dnsNMM.install(it) }
    ///???????????????
    val clipboardNMM = ClipboardNMM().also { dnsNMM.install(it) }
    ///????????????
    val deviceNMM = DeviceNMM().also { dnsNMM.install(it) }
    ///??????
    val locationNMM = LocationNMM().also { dnsNMM.install(it) }
    /// ??????
    val bluetoothNMM = BluetoothNMM().also { dnsNMM.install(it) }
    /// ??????
    val networkNMM = NetworkNMM().also { dnsNMM.install(it) }
    ///??????
    val permissionNMM = PermissionsNMM().also { dnsNMM.install(it) }
    ///????????????
    val fileSystemNMM = FileSystemNMM().also { dnsNMM.install(it) }
    /// NFC
    val nfcNMM = NfcNMM().also { dnsNMM.install(it) }
    /// ??????
    val notificationNMM = NotificationNMM().also { dnsNMM.install(it) }
    /// ??????
    val toastNMM = ToastNMM().also { dnsNMM.install(it) }
    /// ??????
    val shareNMM = ShareNMM().also { dnsNMM.install(it) }
    /// ????????????
    val hapticsNMM = HapticsNMM().also { dnsNMM.install(it) }
    /// ?????????
    val torchNMM = TorchNMM().also() { dnsNMM.install(it) }

    /// SplashScreen
    val splashScreenNMM = SplashScreenNMM().also { dnsNMM.install(it) }
    /// keyboard
    val keyboardNMM = KeyboardNMM().also { dnsNMM.install(it) }
    /// statusBar
    val statusBarNMM = StatusBarNMM().also { dnsNMM.install(it) }
    ///navigation
    val navigationBarNMM = NavigationBarNMM().also { dnsNMM.install(it) }

    /// ??????Jmm
    val jmmNMM = JmmNMM().also { dnsNMM.install(it) }

    /// ??????????????????
    val desktopJMM = DesktopJMM().also { dnsNMM.install(it) }
    val cotJMM = CotJMM().also { dnsNMM.install(it) }
    val cotDemoJMM = CotDemoJMM().also { dnsNMM.install(it) }
    val toyJMM = ToyJMM().also { dnsNMM.install(it) }


    println(Build.MANUFACTURER + "::" + Build.MODEL)

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
            cotDemoJMM.mmid,
        )
        else -> listOf(
            cotDemoJMM.mmid,
        )
    }

    /// ????????????
    val bootNMM = BootNMM(
        bootMmidList
    ).also { dnsNMM.install(it) }

    /// ??????
    dnsNMM.bootstrap()
}