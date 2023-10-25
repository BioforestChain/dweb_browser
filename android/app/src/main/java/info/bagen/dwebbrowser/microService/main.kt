package info.bagen.dwebbrowser.microService

import android.webkit.WebView
import info.bagen.dwebbrowser.microService.browser.desk.DeskNMM
import info.bagen.dwebbrowser.microService.sys.biometrics.BiometricsNMM
import info.bagen.dwebbrowser.microService.sys.clipboard.ClipboardNMM
import info.bagen.dwebbrowser.microService.sys.config.ConfigNMM
import info.bagen.dwebbrowser.microService.sys.device.DeviceNMM
import info.bagen.dwebbrowser.microService.sys.fileSystem.FileSystemNMM
import info.bagen.dwebbrowser.microService.sys.haptics.HapticsNMM
import info.bagen.dwebbrowser.microService.sys.installNativeFetchSysFile
import info.bagen.dwebbrowser.microService.sys.notification.NotificationNMM
import info.bagen.dwebbrowser.microService.sys.permission.PermissionsNMM
import info.bagen.dwebbrowser.microService.sys.share.ShareNMM
import info.bagen.dwebbrowser.microService.sys.toast.ToastNMM
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.FileStorage
import org.dweb_browser.browser.download.DownloadNMM
import org.dweb_browser.browser.jmm.JmmNMM
import org.dweb_browser.browser.jsProcess.JsProcessNMM
import org.dweb_browser.browser.mwebview.MultiWebViewNMM
import org.dweb_browser.browser.nativeui.torch.TorchNMM
import org.dweb_browser.browser.web.BrowserNMM
import org.dweb_browser.browser.zip.ZipNMM
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.dns.nativeFetchAdaptersManager
import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.core.std.http.HttpNMM
import org.dweb_browser.core.sys.boot.BootNMM
import org.dweb_browser.helper.addDebugTags
import org.dweb_browser.helper.platform.getKtorClientEngine
import org.dweb_browser.shared.microService.sys.motionSensors.MotionSensorsNMM
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
  "file",
  "SplashScreen",
  "js-process",
  "desk",
  "JsMM",
  "http",
   */
  when (DEVELOPER.CURRENT) {
    DEVELOPER.GAUBEE -> addDebugTags(listOf("/.+/"))
    DEVELOPER.WaterBang -> addDebugTags(listOf("/.+/"))
    else -> addDebugTags(listOf())
  }

  /// 安装文件请求服务
  installNativeFetchSysFile()
  /// 初始化DNS服务
  val dnsNMM = DnsNMM()

  /// 安装系统应用
  val jsProcessNMM = JsProcessNMM().also { dnsNMM.install(it) }
  val multiWebViewNMM = MultiWebViewNMM().also { dnsNMM.install(it) }
  val httpNMM = HttpNMM().also {
    dnsNMM.install(it)
    /// 自定义 httpClient 的缓存
    HttpClient(getKtorClientEngine()) {
      install(HttpCache) {
        val cacheFile = File(it.getAppContext().cacheDir, "http-fetch.cache")
        publicStorage(FileStorage(cacheFile))
      }
      install(HttpTimeout) {
        requestTimeoutMillis = 10000L
        connectTimeoutMillis = 5000L
      }
    }.also { client ->
      nativeFetchAdaptersManager.setClientProvider(client)
    }
  }

  /// 安装系统桌面
  val browserNMM = BrowserNMM().also { dnsNMM.install(it) }

  /// 下载功能
  val downloadNMM = DownloadNMM().also { dnsNMM.install(it) }
  val zipNMM = ZipNMM().also { dnsNMM.install(it) }

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
  val permissionNMM = PermissionsNMM().also { dnsNMM.install(it) }
  ///文件系统
  val fileSystemNMM = FileSystemNMM().also { dnsNMM.install(it) }
  // 标准文件模块
  val fileNMM = FileNMM().also { dnsNMM.install(it) }
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
  /// 运动传感器
  val motionSensorsNMM = MotionSensorsNMM().also { dnsNMM.install(it) }

  /// NativeUi 是将众多原生UI在一个视图中组合的复合组件
  val nativeUiNMM = org.dweb_browser.browser.nativeui.NativeUiNMM().also { dnsNMM.install(it) }

  /// 安装Jmm
  val jmmNMM = JmmNMM().also { dnsNMM.install(it) }
  val deskNMM = DeskNMM().also { dnsNMM.install(it) }

  /// 启动程序
  val bootNMM = BootNMM(
    listOf(
      fileNMM.mmid,//
      jmmNMM.mmid,//
      httpNMM.mmid,//
      nativeUiNMM.mmid,//
      deskNMM.mmid,//
    ),
  ).also { dnsNMM.install(it) }

  /// 启动Web调试
  WebView.setWebContentsDebuggingEnabled(true)

  /// 启动
  dnsNMM.bootstrap()
  return dnsNMM
}
