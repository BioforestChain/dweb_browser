package info.bagen.dwebbrowser

import android.webkit.WebView
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import org.dweb_browser.browser.common.barcode.QRCodeScanNMM
import org.dweb_browser.browser.desk.DeskNMM
import org.dweb_browser.browser.download.DownloadNMM
import org.dweb_browser.browser.jmm.JmmNMM
import org.dweb_browser.browser.jsProcess.JsProcessNMM
import org.dweb_browser.browser.mwebview.MultiWebViewNMM
import org.dweb_browser.browser.nativeui.NativeUiNMM
import org.dweb_browser.browser.nativeui.torch.TorchNMM
import org.dweb_browser.browser.web.BrowserNMM
import org.dweb_browser.browser.zip.ZipNMM
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.dns.nativeFetchAdaptersManager
import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.core.std.http.HttpNMM
import org.dweb_browser.core.std.http.MultipartNMM
import org.dweb_browser.helper.addDebugTags
import org.dweb_browser.helper.debugTest
import org.dweb_browser.pure.http.engine.getKtorClientEngine
import org.dweb_browser.sys.biometrics.BiometricsNMM
import org.dweb_browser.sys.boot.BootNMM
import org.dweb_browser.sys.camera.CameraNMM
import org.dweb_browser.sys.clipboard.ClipboardNMM
import org.dweb_browser.sys.configure.ConfigNMM
import org.dweb_browser.sys.device.DeviceNMM
import org.dweb_browser.sys.haptics.HapticsNMM
import org.dweb_browser.sys.location.LocationNMM
import org.dweb_browser.sys.media.MediaNMM
import org.dweb_browser.sys.motionSensors.MotionSensorsNMM
import org.dweb_browser.sys.notification.NotificationNMM
import org.dweb_browser.sys.permission.PermissionApplicantTMM
import org.dweb_browser.sys.permission.PermissionNMM
import org.dweb_browser.sys.permission.PermissionProviderTNN
import org.dweb_browser.sys.scan.ScanningNMM
import org.dweb_browser.sys.share.ShareNMM
import org.dweb_browser.sys.toast.ToastNMM

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
  /*when (DEVELOPER.CURRENT) {
    DEVELOPER.GAUBEE -> addDebugTags(listOf("/.+/"))
    DEVELOPER.WaterbangXiaoMi -> addDebugTags(listOf("/.+/"))
    DEVELOPER.WaterBang -> addDebugTags(listOf("/.+/"))
    DEVELOPER.Kingsword09 -> addDebugTags(listOf("/.+/"))
    DEVELOPER.HLOppo -> addDebugTags(listOf("/.+/"))
    else -> addDebugTags(listOf())
  }*/
  if (BuildConfig.DEBUG) addDebugTags(listOf("/.+/"))

  /// 初始化DNS服务
  val dnsNMM = DnsNMM()
  suspend fun MicroModule.setup() = this.also {
    dnsNMM.install(this)
  }

  val permissionNMM = PermissionNMM().setup()

  /// 安装系统应用
  val jsProcessNMM = JsProcessNMM().setup()
  val multiWebViewNMM = MultiWebViewNMM().setup()
  val httpNMM = HttpNMM().also { it ->
    dnsNMM.install(it)
    /// 自定义 httpClient 的缓存
    HttpClient(getKtorClientEngine()) {
      install(HttpTimeout) {
        // requestTimeoutMillis = 600_000L
        connectTimeoutMillis = 5000L
      }
      install(WebSockets)
    }.also { client ->
      nativeFetchAdaptersManager.setClientProvider(client)
    }
  }

  /// 安装系统桌面
  val browserNMM = BrowserNMM().setup()

  /// 下载功能
  val downloadNMM = DownloadNMM().setup()
  val zipNMM = ZipNMM().setup()

  val cameraNMM = CameraNMM().setup()
  /// 扫码
  val scannerNMM = ScanningNMM().setup()
  val qrCodeScanNMM = QRCodeScanNMM().setup()
  ///安装剪切板
  val clipboardNMM = ClipboardNMM().setup()
  ///设备信息
  val deviceNMM = DeviceNMM().setup()
  val configNMM = ConfigNMM().setup()
  ///位置
  val locationNMM = LocationNMM().setup()
//    /// 蓝牙
//    val bluetoothNMM = BluetoothNMM().setup()
  // 标准文件模块
  val fileNMM = FileNMM().setup()
  /// NFC
//  val nfcNMM = NfcNMM().setup()
  /// 通知
  val notificationNMM = NotificationNMM().setup()
  /// 弹窗
  val toastNMM = ToastNMM().setup()
  /// 分享
  val shareNMM = ShareNMM().setup()
  /// 振动效果
  val hapticsNMM = HapticsNMM().setup()
  /// 手电筒
  val torchNMM = TorchNMM().also() { dnsNMM.install(it) }
  /// 生物识别
  val biometricsNMM = BiometricsNMM().setup()
  /// 运动传感器
  val motionSensorsNMM = MotionSensorsNMM().setup()
  /// 媒体操作
  val mediaNMM = MediaNMM().setup()
  /// multipart
  val multipartNMM = MultipartNMM().setup()

  /// NativeUi 是将众多原生UI在一个视图中组合的复合组件
  val nativeUiNMM = NativeUiNMM().setup()

  /// 安装Jmm
  val jmmNMM = JmmNMM().setup()
  val deskNMM = DeskNMM().setup()

  /// 启动程序
  val bootNMM = BootNMM(
    listOf(
      fileNMM.mmid,//
      permissionNMM.mmid,// 权限管理
      httpNMM.mmid,//
      jmmNMM.mmid,//
      nativeUiNMM.mmid,//
      downloadNMM.mmid, // 为了获取下载的数据
      browserNMM.mmid, // 为了启动后能够顺利加载添加到桌面的哪些数据，不加载browser界面
      deskNMM.mmid,//
      locationNMM.mmid,
      qrCodeScanNMM.mmid,
    ),
  ).setup()

  if (debugTest.isEnable) {
    PermissionProviderTNN().setup()
    PermissionApplicantTMM().setup()
  }

  /// 启动Web调试
  WebView.setWebContentsDebuggingEnabled(true)

  /// 启动
  dnsNMM.bootstrap()
  return dnsNMM
}
