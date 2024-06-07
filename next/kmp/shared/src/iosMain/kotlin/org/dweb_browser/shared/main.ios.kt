@file:Suppress("FunctionName", "unused")

package org.dweb_browser.shared

//import org.dweb_browser.sys.microphone.MicroPhoneNMM
import org.dweb_browser.browser.desk.DeskNMM
import org.dweb_browser.browser.download.DownloadNMM
import org.dweb_browser.browser.jmm.JmmNMM
import org.dweb_browser.browser.jsProcess.JsProcessNMM
import org.dweb_browser.browser.mwebview.MultiWebViewNMM
import org.dweb_browser.browser.nativeui.torch.TorchNMM
import org.dweb_browser.browser.web.BrowserNMM
import org.dweb_browser.browser.zip.ZipNMM
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.nativeMicroModuleUIApplication
import org.dweb_browser.core.std.boot.BootNMM
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.core.std.http.HttpNMM
import org.dweb_browser.core.std.http.MultipartNMM
import org.dweb_browser.helper.addDebugTags
import org.dweb_browser.helper.debugTest
import org.dweb_browser.helper.platform.DeepLinkHook.Companion.deepLinkHook
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.sys.biometrics.BiometricsNMM
import org.dweb_browser.sys.clipboard.ClipboardNMM
import org.dweb_browser.sys.configure.ConfigNMM
import org.dweb_browser.sys.contact.ContactNMM
import org.dweb_browser.sys.device.DeviceNMM
import org.dweb_browser.sys.filechooser.FileChooserNMM
import org.dweb_browser.sys.haptics.HapticsNMM
import org.dweb_browser.sys.location.LocationNMM
import org.dweb_browser.sys.media.MediaNMM
import org.dweb_browser.sys.mediacapture.MediaCaptureNMM
import org.dweb_browser.sys.motionSensors.MotionSensorsNMM
import org.dweb_browser.sys.notification.NotificationNMM
import org.dweb_browser.sys.permission.PermissionApplicantTMM
import org.dweb_browser.sys.permission.PermissionNMM
import org.dweb_browser.sys.permission.PermissionProviderTNN
import org.dweb_browser.sys.scan.ScanningNMM
import org.dweb_browser.sys.share.ShareNMM
import org.dweb_browser.sys.shortcut.ShortcutNMM
import org.dweb_browser.sys.toast.ToastNMM
import platform.UIKit.UIApplication

val dwebViewController = nativeViewController
val dwebDeepLinkHook = deepLinkHook
private lateinit var dnsNMM: DnsNMM

suspend fun dnsFetch(url: String): PureResponse {
  return dnsNMM.runtime.nativeFetch(url)
}

@Suppress("UNUSED_VARIABLE")
suspend fun startDwebBrowser(
  app: UIApplication, debugMode: Boolean, debugTags: List<String> = listOf("/.+/")
): DnsNMM {
  nativeMicroModuleUIApplication = app;

  if (debugMode) {
    addDebugTags(debugTags)
  }

  /// 初始化DNS服务
  dnsNMM = DnsNMM()

  suspend fun <T : MicroModule> T.setup() = this.also {
    dnsNMM.install(this)
  }

  val permissionNMM = PermissionNMM().setup()

  /// 安装系统应用
  val jsProcessNMM = JsProcessNMM().setup()
  val multiWebViewNMM = MultiWebViewNMM().setup()
  val httpNMM = HttpNMM().also { it ->
    dnsNMM.install(it)
  }

  /// 下载功能
  val downloadNMM = DownloadNMM().setup()
//  val microNMM = MicroPhoneNMM().setup()

  val zipNMM = ZipNMM().setup()

  val mediaCaptureNMM = MediaCaptureNMM().setup()
  /// 扫码
  val scannerNMM = ScanningNMM().setup()
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
  /// Contact
  val contactNMM = ContactNMM().setup()
  /// shortcut
  val shortcutNMM = ShortcutNMM().setup()
  val fileChooserNMM = FileChooserNMM().setup()

  /// 安装Jmm
  val jmmNMM = JmmNMM().setup()
  val deskNMM = DeskNMM().setup()

  val browserNMM = BrowserNMM().setup()

  /// 启动程序
  val bootNMM = BootNMM(
    listOf(
      permissionNMM.mmid,// 权限管理
      fileNMM.mmid,//
      jmmNMM.mmid,//
      httpNMM.mmid,//
      downloadNMM.mmid, // 为了获取下载的数据
      deskNMM.mmid,//
      browserNMM.mmid, // 为了启动后能够顺利加载添加到桌面的哪些数据，不加载browser界面

    ),
  ).setup()

  if (debugTest.isEnable) {
    PermissionProviderTNN().setup()
    PermissionApplicantTMM().setup()
  }

  /// 启动
  val dnsRuntime = dnsNMM.bootstrap()
  // 启动的时候就开始监听deeplink
  dwebDeepLinkHook.deeplinkSignal.listen {
    dnsRuntime.nativeFetch(it)
  }
  dnsRuntime.boot(bootNMM)
  return dnsNMM
}

