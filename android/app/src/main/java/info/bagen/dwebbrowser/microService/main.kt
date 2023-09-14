package info.bagen.dwebbrowser.microService

import android.webkit.WebView
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.desk.DesktopNMM
import info.bagen.dwebbrowser.microService.browser.jmm.JmmNMM
import info.bagen.dwebbrowser.microService.browser.jsProcess.JsProcessNMM
import info.bagen.dwebbrowser.microService.browser.mwebview.MultiWebViewNMM
import info.bagen.dwebbrowser.microService.browser.nativeui.NativeUiNMM
import info.bagen.dwebbrowser.microService.browser.nativeui.torch.TorchNMM
import info.bagen.dwebbrowser.microService.sys.LocalFileFetch
import info.bagen.dwebbrowser.microService.sys.biometrics.BiometricsNMM
import info.bagen.dwebbrowser.microService.sys.clipboard.ClipboardNMM
import info.bagen.dwebbrowser.microService.sys.config.ConfigNMM
import info.bagen.dwebbrowser.microService.sys.device.DeviceNMM
import info.bagen.dwebbrowser.microService.sys.haptics.HapticsNMM
import info.bagen.dwebbrowser.microService.sys.notification.NotificationNMM
import info.bagen.dwebbrowser.microService.sys.share.ShareNMM
import info.bagen.dwebbrowser.microService.sys.toast.ToastNMM
import info.bagen.dwebbrowser.microService.sys.window.WindowNMM
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.FileStorage
import org.dweb_browser.browserUI.microService.browser.web.BrowserNMM
import org.dweb_browser.helper.addDebugTags
import org.dweb_browser.helper.platform.getKtorClientEngine
import org.dweb_browser.microservice.sys.boot.BootNMM
import org.dweb_browser.microservice.sys.dns.DnsNMM
import org.dweb_browser.microservice.sys.dns.nativeFetchAdaptersManager
import org.dweb_browser.microservice.sys.http.HttpNMM
import org.dweb_browser.shared.Greeting
import java.io.File

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
  "dwebview",
  "native-ipc" ,
  "browser",
  "jmm",
  "SplashScreen",
  "js-process",
  "desk",
   */
  when (DEVELOPER.CURRENT) {
    DEVELOPER.GAUBEE -> addDebugTags(
      listOf<String>(
        "JsMM",
        "http",
        "/.+/",
//        "fetch",
//        "message-port-ipc"
      )
    )

    DEVELOPER.HuangLin, DEVELOPER.HLVirtual, DEVELOPER.HLOppo, DEVELOPER.HBXiaomi, DEVELOPER.ZGSansung -> addDebugTags(
      listOf(
        "fetch",
        "http",
        "mwebview",
        "fetch-file",
        "js-process",
        "browser",
        "desk",
        "JMM",
        "window",
        "dwebview"
      )
    )

    DEVELOPER.WaterBang -> addDebugTags(
      listOf(
        "dwebview",
        "mwebview",
        "http",
        "JsMM",
        "js-process",
        "DNS",
        "desk",
        "browser",
        "JMM",
        "/.+/",
      )
    )

    DEVELOPER.Kingsword09, DEVELOPER.KVirtual -> addDebugTags(
      listOf(
        "desk",
        "/.+/",
      )
    )

    else -> addDebugTags(
      listOf("desk", "mwebview", "fetch")
    )
  }

  LocalFileFetch.INSTANCE // 注入 localFileFetch
  val dnsNMM = DnsNMM()

  /// 安装系统应用
  val jsProcessNMM = JsProcessNMM().also { dnsNMM.install(it) }
  val multiWebViewNMM = MultiWebViewNMM().also { dnsNMM.install(it) }
  val httpNMM = HttpNMM().also {
    dnsNMM.install(it)
    /// 自定义 httpClient 的缓存
    HttpClient(getKtorClientEngine()) {
      install(HttpCache) {
        val cacheFile = File(App.appContext.cacheDir, "http-fetch.cache")
        publicStorage(FileStorage(cacheFile))
      }
      install(HttpTimeout) {
        requestTimeoutMillis = 5000L
        connectTimeoutMillis = 5000L
      }
    }.also { client ->
      nativeFetchAdaptersManager.setClientProvider(client)
    }
  }

  /// 安装系统桌面
  val browserNMM = BrowserNMM().also { dnsNMM.install(it) }

  /// 扫码
  val scannerNMM = info.bagen.dwebbrowser.microService.sys.barcodeScanning.ScanningNMM()
    .also { dnsNMM.install(it) }
  ///安装剪切板
  val clipboardNMM = ClipboardNMM().also { dnsNMM.install(it) }
  ///设备信息
  val deviceNMM = DeviceNMM().also { dnsNMM.install(it) }
  val configNMM = ConfigNMM().also { dnsNMM.install(it) }
  ///位置
//  val locationNMM = LocationNMM().also { dnsNMM.install(it) }
//    /// 蓝牙
//    val bluetoothNMM = BluetoothNMM().also { dnsNMM.install(it) }
//    ///权限
//    val permissionNMM = PermissionsNMM().also { dnsNMM.install(it) }
  ///文件系统
//  val fileSystemNMM = FileSystemNMM().also { dnsNMM.install(it) }
  /// NFC
//  val nfcNMM = NfcNMM().also { dnsNMM.install(it) }
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
  val nativeUiNMM = NativeUiNMM().also { dnsNMM.install(it) }

  /// 安装Jmm
  val jmmNMM = JmmNMM().also { dnsNMM.install(it) }
  val desktopNMM = DesktopNMM().also { dnsNMM.install(it) }
  val windowNMM = WindowNMM().also { dnsNMM.install(it) }

  /**
   *
   * browserNMM.mmid,
   * desktopJMM.mmid,
   * plaocDemoJMM.mmid,
   * cotJMM.mmid,
   * toyJMM.mmid,
   */
  val bootMmidList = when (DEVELOPER.CURRENT) {
    DEVELOPER.HLOppo, DEVELOPER.GAUBEE -> listOf(
      desktopNMM.mmid,
//            browserNMM.mmid,
    )

    DEVELOPER.HuangLin, DEVELOPER.HLVirtual, DEVELOPER.HBXiaomi, DEVELOPER.ZGSansung -> listOf(
      browserNMM.mmid,
      // desktopNMM.mmid,
    )

    DEVELOPER.WaterBang -> listOf(
//      browserNMM.mmid,
      desktopNMM.mmid,
    )

    DEVELOPER.Kingsword09, DEVELOPER.KVirtual -> listOf(
//            browserNMM.mmid,
      desktopNMM.mmid,
    )

    else -> listOf(desktopNMM.mmid)
  }

  /// 启动程序
  val bootNMM = BootNMM(
    bootMmidList.plus(jmmNMM.mmid).plus(httpNMM.mmid).plus(nativeUiNMM.mmid),
  ).also { dnsNMM.install(it) }

  /// 启动Web调试
  WebView.setWebContentsDebuggingEnabled(true)

  println("!!!!!" + Greeting().greet())

  /// 启动
  dnsNMM.bootstrap()
  return dnsNMM
}
