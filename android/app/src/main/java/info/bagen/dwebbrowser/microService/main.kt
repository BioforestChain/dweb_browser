package info.bagen.dwebbrowser.microService

import android.webkit.WebView
import info.bagen.dwebbrowser.microService.browser.BrowserNMM
import org.dweb_browser.helper.*
import org.dweb_browser.microservice.sys.boot.BootNMM
import org.dweb_browser.microservice.sys.dns.DnsNMM
import org.dweb_browser.microservice.sys.http.HttpNMM
import info.bagen.dwebbrowser.microService.browser.jmm.JmmNMM
import info.bagen.dwebbrowser.microService.browser.jsProcess.JsProcessNMM
import info.bagen.dwebbrowser.microService.browser.mwebview.MultiWebViewNMM
import info.bagen.dwebbrowser.microService.browser.nativeui.NativeUiNMM
import info.bagen.dwebbrowser.microService.browser.nativeui.torch.TorchNMM
import info.bagen.dwebbrowser.microService.sys.LocalFileFetch
import info.bagen.dwebbrowser.microService.sys.biometrics.BiometricsNMM
import info.bagen.dwebbrowser.microService.sys.clipboard.ClipboardNMM
import info.bagen.dwebbrowser.microService.sys.device.*
import info.bagen.dwebbrowser.microService.sys.fileSystem.FileSystemNMM
import info.bagen.dwebbrowser.microService.sys.haptics.HapticsNMM
import info.bagen.dwebbrowser.microService.sys.notification.NotificationNMM
import info.bagen.dwebbrowser.microService.sys.share.ShareNMM
import info.bagen.dwebbrowser.microService.sys.toast.ToastNMM
import info.bagen.dwebbrowser.microService.test.DesktopDemoJMM
import info.bagen.dwebbrowser.microService.test.PlaocDemoJMM

val InternalBranch = when (DEVELOPER.CURRENT) {
  DEVELOPER.GAUBEE, DEVELOPER.HuangLin, DEVELOPER.HLOppo, DEVELOPER.WaterBang, DEVELOPER.HLVirtual -> true
  else -> false
} // 用户临时区分上架时的分支,false为上架的apk

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
  "native-ipc" ,
  "browser",
  "jmm",
  "SplashScreen",
  "js-process"
   */
  when (DEVELOPER.CURRENT) {
    DEVELOPER.GAUBEE -> debugTags.addAll(
      listOf<String>(
        "JsMM",
        "fetch",
        "message-port-ipc"
      )
    )

    DEVELOPER.HuangLin, DEVELOPER.HLVirtual, DEVELOPER.HLOppo, DEVELOPER.HBXiaomi, DEVELOPER.ZGSansung -> debugTags.addAll(
      listOf("Share", "fetch", "http", "JsMM", "browser", "biometrics", "mwebview", "fetch-file", "js-process", "dns", "boot", "stream", "stream-ipc", "message-port-ipc")
    )

    DEVELOPER.WaterBang -> debugTags.addAll(
      listOf( "dwebview", "mwebview","JsMM","http","fetch")
    )

    else -> debugTags.addAll(
      listOf("Share", "browser")
    )
  }

  LocalFileFetch.INSTANCE // 注入 localFileFetch
  val dnsNMM = DnsNMM()

  /// 安装系统应用
  val jsProcessNMM = JsProcessNMM().also { dnsNMM.install(it) }
  val multiWebViewNMM = MultiWebViewNMM().also { dnsNMM.install(it) }
  val httpNMM = HttpNMM().also { dnsNMM.install(it) }

  /// 安装系统桌面
  val browserNMM = BrowserNMM().also { dnsNMM.install(it) }

  /// 扫码
  val scannerNMM = info.bagen.dwebbrowser.microService.sys.barcodeScanning.ScanningNMM()
    .also { dnsNMM.install(it) }
  ///安装剪切板
  val clipboardNMM = ClipboardNMM().also { dnsNMM.install(it) }
  ///设备信息
  val deviceNMM = DeviceNMM().also { dnsNMM.install(it) }
  ///位置
  val locationNMM = LocationNMM().also { dnsNMM.install(it) }
//    /// 蓝牙
//    val bluetoothNMM = BluetoothNMM().also { dnsNMM.install(it) }
//    ///权限
//    val permissionNMM = PermissionsNMM().also { dnsNMM.install(it) }
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
  /// 生物识别
  val biometricsNMM = BiometricsNMM().also { dnsNMM.install(it) }

  /// NativeUi 是将众多原生UI在一个视图中组合的复合组件
  NativeUiNMM().also { dnsNMM.install(it) }

  /// 安装Jmm
  val jmmNMM = JmmNMM().also { dnsNMM.install(it) }
  // 测试使用，打包成apk需要删除
  val plaocDemoJMM = PlaocDemoJMM().also { dnsNMM.install(it) }
  val desktopDemoJMM = DesktopDemoJMM().also { dnsNMM.install(it) }
  /**
   *
   * browserNMM.mmid,
   * desktopJMM.mmid,
   * plaocDemoJMM.mmid,
   * cotJMM.mmid,
   * toyJMM.mmid,
   */
  val bootMmidList = when (DEVELOPER.CURRENT) {
    DEVELOPER.GAUBEE -> listOf(
//            cotJMM.mmid,
      browserNMM.mmid,
//            browserNMM.mmid,
    )

    DEVELOPER.HuangLin, DEVELOPER.HLVirtual, DEVELOPER.HLOppo, DEVELOPER.HBXiaomi, DEVELOPER.ZGSansung -> listOf(
      /*browserNMM.mmid,*/ plaocDemoJMM.mmid
    )

    DEVELOPER.WaterBang -> listOf(
//      browserNMM.mmid,
      plaocDemoJMM.mmid,
//      desktopDemoJMM.mmid,
    )

    DEVELOPER.Kingsword09 -> listOf(
//            browserNMM.mmid,
//            desktopJMM.mmid
    )

    else -> listOf(browserNMM.mmid)
  }

  /// 启动程序
  val bootNMM = BootNMM(
    bootMmidList
  ).also { dnsNMM.install(it) }

  /// 启动Web调试
  WebView.setWebContentsDebuggingEnabled(true)

  /// 启动
  dnsNMM.bootstrap()
  return dnsNMM
}
