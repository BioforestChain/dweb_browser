@file:Suppress("FunctionName", "unused")

package org.dweb_browser.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.core.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.dweb_browser.browser.desk.DeskNMM
import org.dweb_browser.browser.download.DownloadNMM
import org.dweb_browser.browser.jmm.JmmNMM
import org.dweb_browser.browser.jsProcess.JsProcessNMM
import org.dweb_browser.browser.mwebview.MultiWebViewNMM
import org.dweb_browser.browser.nativeui.torch.TorchNMM
import org.dweb_browser.browser.web.BrowserNMM
import org.dweb_browser.browser.web.doSearch
import org.dweb_browser.browser.web.iOSMainView
import org.dweb_browser.browser.zip.ZipNMM
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.PureStreamBody
import org.dweb_browser.core.ipc.helper.IpcHeaders
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.nativeMicroModuleUIApplication
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.dns.nativeFetchAdaptersManager
import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.core.std.http.HttpNMM
import org.dweb_browser.core.std.websocketClient.WebSocketClientNMM
import org.dweb_browser.dwebview.DWebMessage
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.addDebugTags
import org.dweb_browser.helper.debugTest
import org.dweb_browser.helper.platform.DeepLinkHook.Companion.deepLinkHook
import org.dweb_browser.helper.platform.LocalPureViewBox
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController
import org.dweb_browser.helper.platform.PureViewBox
import org.dweb_browser.helper.platform.getKtorClientEngine
import org.dweb_browser.helper.toBase64ByteArray
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.sys.KmpNativeBridgeEventSender
import org.dweb_browser.sys.biometrics.BiometricsNMM
import org.dweb_browser.sys.boot.BootNMM
import org.dweb_browser.sys.clipboard.ClipboardNMM
import org.dweb_browser.sys.configure.ConfigNMM
import org.dweb_browser.sys.device.DeviceNMM
import org.dweb_browser.sys.haptics.HapticsNMM
import org.dweb_browser.sys.motionSensors.MotionSensorsNMM
import org.dweb_browser.sys.notification.NotificationNMM
import org.dweb_browser.sys.permission.PermissionApplicantTMM
import org.dweb_browser.sys.permission.PermissionNMM
import org.dweb_browser.sys.permission.PermissionProviderTNN
import org.dweb_browser.sys.scan.ScanningNMM
import org.dweb_browser.sys.share.ShareNMM
import org.dweb_browser.sys.toast.ToastNMM
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.WindowPreviewer
import org.dweb_browser.sys.window.render.watchedState
import platform.CoreGraphics.CGFloat
import platform.UIKit.UIApplication
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.WebKit.WKWebViewConfiguration

val dwebViewController = nativeViewController
val dwebDeepLinkHook = deepLinkHook
suspend fun startDwebBrowser(app: UIApplication, debugMode: Boolean): DnsNMM {
  nativeMicroModuleUIApplication = app;

  if (debugMode) {
    addDebugTags(listOf("/.+/"))
  }

  /// 初始化DNS服务
  val dnsNMM = DnsNMM().also { dnsNMM ->
    // 启动的时候就开始监听deeplink
    dwebDeepLinkHook.deeplinkSignal.listen {
      dnsNMM.nativeFetch(it)
    }
  }
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

  /// 下载功能
  val downloadNMM = DownloadNMM().setup()
  val zipNMM = ZipNMM().setup()

  /// 扫码
  val scannerNMM = ScanningNMM().setup()
  ///安装剪切板
  val clipboardNMM = ClipboardNMM().setup()
  ///设备信息
  val deviceNMM = DeviceNMM().setup()
  val configNMM = ConfigNMM().setup()
  ///位置
//  val locationNMM = LocationNMM().setup()
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

  /// websocket-client
  val webSocketClientNMM = WebSocketClientNMM().setup()


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
    ),
  ).setup()

  if (debugTest.isEnable) {
    PermissionProviderTNN().setup()
    PermissionApplicantTMM().setup()
  }

  /// 启动
  dnsNMM.bootstrap()
  return dnsNMM
}


public fun regiserIosMainView(iosView: ()->UIView) {
  iOSMainView = iosView
}

public fun regiserIosSearch(search: (String)->Unit) {
  doSearch = search
}

@OptIn(ExperimentalForeignApi::class)
@Composable
fun PreviewWindowTopBar(iosView: UIView, onSizeChange: (CGFloat, CGFloat) -> Unit) {

  var winController: WindowController? = null
  CompositionLocalProvider(LocalPureViewBox provides PureViewBox(LocalUIViewController.current)) {
    WindowPreviewer(modifier = Modifier.width(350.dp).height(500.dp), config = {
      state.title = "应用长长的标题的标题的标题～～"
      state.topBarContentColor = "#FF00FF"
      state.themeColor = "#Fd9F9F"
      state.iconUrl = "http://172.30.92.50:12207/m3-favicon-apple-touch.png"
      state.iconMaskable = true
      state.showMenuPanel = true
      winController = this
    }) { modifier ->

      val colorScheme by winController?.watchedState { colorScheme } ?: return@WindowPreviewer
      KmpNativeBridgeEventSender.sendColorScheme(colorScheme.scheme)

      Box() {
        val scope = rememberCoroutineScope()
        onSizeChange(width.toDouble(), height.toDouble())
        UIKitView(
          factory = {
            iosView
          },
          modifier = Modifier,
          update = { view ->
            println("update:::: $view")
          })
      }
    }
  }
}

@OptIn(ExperimentalForeignApi::class)
@Suppress("FunctionName", "unused")
fun MainViewController(
  iosView: UIView,
  onSizeChange: (CGFloat, CGFloat) -> Unit
): UIViewController {

  val dwebServer = Http1ServerTest()
  val httpHandler: suspend (PureRequest) -> PureResponse = { request ->
    if (request.url.host == "localhost:20222") {
      println("httpHandler: ${request.url.encodedPath}")
    }

    if (request.url.encodedPath == "/index.html") {
      val response = PureResponse(
        HttpStatusCode.OK,
        IpcHeaders(mapOf("Content-Type" to "text/html;charset=UTF-8")),
        PureStreamBody(
          """
        <h1>你好!!</h1>
                    <h1>你好!!</h1>
                    <h1>你好!!</h1>
                    <h1>你好!!</h1>
                    <h1>你好!!</h1>
                    <h1>你好!!</h1>
                    <img src='./hi.png'/>
                    <script>
                    var a = 1;
                    addEventListener('message',(event)=>{
                        const ele = document.createElement("h1");
                        ele.style.color = 'red';
                        ele.innerHTML = [event.data,...event.ports].join(" ");
                        document.body.insertBefore(ele, document.body.firstChild);
                    });
                    </script>
      """.trimIndent().toByteArray()
        )
      )

      response
    } else if (request.url.encodedPath == "/hi.png") {
      val response = PureResponse(
        HttpStatusCode.OK,
        IpcHeaders(mapOf("Content-Type" to "image/svg+xml")),
        PureStreamBody(
          "PHN2ZyB3aWR0aD0iMzMzIiBoZWlnaHQ9IjIxMCIgdmlld0JveD0iMCAwIDMzMyAyMTAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxwYXRoIGQ9Ik05MC45NDI5IDMxLjA3NzlWOTYuMjk5NSIgc3Ryb2tlPSIjMjEyMTIxIiBzdHJva2UtbWl0ZXJsaW1pdD0iMTAiIHN0cm9rZS1kYXNoYXJyYXk9IjQgNiIvPgo8cGF0aCBkPSJNMjA2LjgwNiAxNzEuMjY2TDE4Ni43MzMgMTg4Ljg1OEwyNy42MzI2IDkzLjM4OTFMNDcuNjk4MyA3NS44MDRIMjA2LjgwNlYxNzEuMjY2WiIgZmlsbD0id2hpdGUiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0xODYuNzMzIDkzLjM4OTJIMjcuNjMyNlYxODguODUxSDE4Ni43MzNWOTMuMzg5MloiIGZpbGw9IndoaXRlIiBzdHJva2U9IiMyMTIxMjEiIHN0cm9rZS1taXRlcmxpbWl0PSIxMCIvPgo8cGF0aCBkPSJNMTA3LjE4NSAxNjIuOTA2QzEyMC45MSAxNjIuOTA2IDEzMi4wMzYgMTUzLjE1NSAxMzIuMDM2IDE0MS4xMjdDMTMyLjAzNiAxMjkuMDk5IDEyMC45MSAxMTkuMzQ4IDEwNy4xODUgMTE5LjM0OEM5My40NjA2IDExOS4zNDggODIuMzM0NSAxMjkuMDk5IDgyLjMzNDUgMTQxLjEyN0M4Mi4zMzQ1IDE1My4xNTUgOTMuNDYwNiAxNjIuOTA2IDEwNy4xODUgMTYyLjkwNloiIGZpbGw9IiM3QUY3QzIiLz4KPHBhdGggZD0iTTExOS41MjIgMTM0LjIwNUwxMDMuNzQxIDE0OC4wNDJMOTUuODUwMyAxNDEuMTI3IiBzdHJva2U9IiMyMTIxMjEiIHN0cm9rZS13aWR0aD0iNSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0xODYuNzMzIDkzLjM4OTFMMjA2LjgwNyA3NS44MDQiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0yMjYuODczIDc1LjgwNEwyNDYuOTQ2IDU4LjIxMjJMMjA2LjgwNyA3NS44MDRMMTg2LjczMyA5My4zODkyTDIyNi44NzMgNzUuODA0WiIgZmlsbD0id2hpdGUiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiBzdHJva2UtbGluZWpvaW49InJvdW5kIi8+CjxwYXRoIGQ9Ik0yOTEuNDk5IDE5MS4zODRMMjcxLjQzMyAyMDguOTY5TDE1Mi40NjQgMTM5Ljg4NEwxNzIuNTMgMTIyLjI5OUgyOTEuNDk5VjE5MS4zODRaIiBmaWxsPSJ3aGl0ZSIgc3Ryb2tlPSIjMjEyMTIxIiBzdHJva2UtbWl0ZXJsaW1pdD0iMTAiLz4KPHBhdGggZD0iTTI3MS40MzMgMTM5Ljg4NEgxNTIuNDY0VjIwOC45NjlIMjcxLjQzM1YxMzkuODg0WiIgZmlsbD0id2hpdGUiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0yMTQuMDg3IDE5MC40MjVDMjI0LjE2OSAxOTAuNDI1IDIzMi4zNDIgMTgzLjI2MiAyMzIuMzQyIDE3NC40MjZDMjMyLjM0MiAxNjUuNTkxIDIyNC4xNjkgMTU4LjQyOCAyMTQuMDg3IDE1OC40MjhDMjA0LjAwNSAxNTguNDI4IDE5NS44MzIgMTY1LjU5MSAxOTUuODMyIDE3NC40MjZDMTk1LjgzMiAxODMuMjYyIDIwNC4wMDUgMTkwLjQyNSAyMTQuMDg3IDE5MC40MjVaIiBmaWxsPSIjMDBDMUE0Ii8+CjxwYXRoIGQ9Ik0yMjMuMTU3IDE2OS4zNDhMMjExLjU2IDE3OS41MTJMMjA1Ljc2NSAxNzQuNDI2IiBzdHJva2U9IiMyMTIxMjEiIHN0cm9rZS13aWR0aD0iNCIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0zMTEuNTY1IDEyMi4yOTlMMzMxLjYzOCAxMDQuNzA3TDI5MS40OTkgMTIyLjI5OUwyNzEuNDMzIDEzOS44ODRMMzExLjU2NSAxMjIuMjk5WiIgZmlsbD0id2hpdGUiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiBzdHJva2UtbGluZWpvaW49InJvdW5kIi8+CjxwYXRoIGQ9Ik0xMjUuODMzIDEyMi4yOTlMMTQ1Ljg5OSAxMDQuNzA3TDE3Mi41MyAxMjIuMjk5TDE1Mi40NjUgMTM5Ljg4NEwxMjUuODMzIDEyMi4yOTlaIiBmaWxsPSJ3aGl0ZSIgc3Ryb2tlPSIjMjEyMTIxIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiLz4KPHBhdGggZD0iTTI3LjYzMjMgOTMuMzg5M0w0Ny42OTgxIDc1LjgwNDJIMTczLjg1NlY5My4zODkzSDI3LjYzMjNaIiBmaWxsPSIjMjEyMTIxIi8+CjxwYXRoIGQ9Ik0xMDMuMjcxIDkyLjUxMTRWNTIuMDczN0g1Ny4xMjkzVjkyLjUxMTRIMTAzLjI3MVoiIGZpbGw9IiNGRkExMDAiLz4KPHBhdGggZD0iTTE2MS4yODkgNTQuMjA5M0wxNzMuODU5IDQzLjE5MjlMMTYxLjI4OSAzMi4xNzY1TDE0OC43MTkgNDMuMTkyOUwxNjEuMjg5IDU0LjIwOTNaIiBmaWxsPSIjMjEyMTIxIiBzdHJva2U9IiMyMTIxMjEiIHN0cm9rZS1taXRlcmxpbWl0PSIxMCIvPgo8cGF0aCBkPSJNOTQuMjgzMyAyOS43NzU2TDk0LjI4ODggMjkuNzcwOUM5Ni4zNzcyIDI3Ljk0MDYgOTYuMzc3MyAyNC45NzMxIDk0LjI4ODkgMjMuMTQyOEw5NC4yODM0IDIzLjEzOEM5Mi4xOTUgMjEuMzA3OCA4OC44MDg4IDIxLjMwNzkgODYuNzIwNCAyMy4xMzgxTDg2LjcxNDkgMjMuMTQyOUM4NC42MjY1IDI0Ljk3MzIgODQuNjI2NSAyNy45NDA2IDg2LjcxNSAyOS43NzA4TDg2LjcyMDQgMjkuNzc1NkM4OC44MDg5IDMxLjYwNTkgOTIuMTk0OSAzMS42MDU5IDk0LjI4MzMgMjkuNzc1NloiIGZpbGw9IiMyMTIxMjEiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0xMjYuMTU4IDkyLjUxMTJDMTI2LjE1OCA4Ni45MzAzIDEyOC42ODggODEuNTc4IDEzMy4xOTEgNzcuNjMxOEMxMzcuNjk0IDczLjY4NTUgMTQzLjgwMSA3MS40Njg1IDE1MC4xNjkgNzEuNDY4NUMxNTYuNTM4IDcxLjQ2ODUgMTYyLjY0NSA3My42ODU1IDE2Ny4xNDggNzcuNjMxOEMxNzEuNjUxIDgxLjU3OCAxNzQuMTggODYuOTMwMyAxNzQuMTggOTIuNTExMiIgZmlsbD0iI0ZGNzZDNCIvPgo8cGF0aCBkPSJNMjgwLjIxOCAxMzEuOTdIMTk5Ljc0N1Y1OC4xMjQzQzE5OS43NDcgNTUuNzk1OSAyMDAuODAyIDUzLjU2MjkgMjAyLjY4MSA1MS45MTY1QzIwNC41NTkgNTAuMjcwMiAyMDcuMTA3IDQ5LjM0NTIgMjA5Ljc2NCA0OS4zNDUySDI3MC4xNzdDMjcyLjgzNCA0OS4zNDUyIDI3NS4zODIgNTAuMjcwMiAyNzcuMjYgNTEuOTE2NUMyNzkuMTM5IDUzLjU2MjkgMjgwLjE5NCA1NS43OTU5IDI4MC4xOTQgNTguMTI0M0wyODAuMjE4IDEzMS45N1oiIGZpbGw9IndoaXRlIi8+CjxwYXRoIGQ9Ik0yNzEuMDE5IDE0MC4yMzRWMzIuMjIxMiIgc3Ryb2tlPSIjMjEyMTIxIiBzdHJva2UtbWl0ZXJsaW1pdD0iMTAiLz4KPHBhdGggZD0iTTI0NS4wNiAxNDAuMjM1VjguNTM0MTgiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0yMTguMDIgMTQwLjIzNVY4LjUzNDE4IiBzdHJva2U9IiMyMTIxMjEiIHN0cm9rZS1taXRlcmxpbWl0PSIxMCIvPgo8cGF0aCBkPSJNMjgwLjc1NCAxMjYuMDIySDE3My42NzQiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0yODAuNzUzIDEwMi4zMzVIMTc2LjkxOCIgc3Ryb2tlPSIjMjEyMTIxIiBzdHJva2UtbWl0ZXJsaW1pdD0iMTAiLz4KPHBhdGggZD0iTTI4MC43NTMgNzguNjQ3OUgxNjguMjY2IiBzdHJva2U9IiMyMTIxMjEiIHN0cm9rZS1taXRlcmxpbWl0PSIxMCIvPgo8cGF0aCBkPSJNMjc5LjY3MiA1NS45MDg0SDE5OS42MzIiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0yODAuMjE3IDMxLjk4MjlIMjAwLjU1NiIgc3Ryb2tlPSIjMjEyMTIxIiBzdHJva2UtbWl0ZXJsaW1pdD0iMTAiLz4KPHBhdGggZD0iTTI1OC4xIDEwNS45ODdMMjU4LjEwNSAxMDUuOTgyQzI2MC4xOTMgMTA0LjE1MiAyNjAuMTkzIDEwMS4xODUgMjU4LjEwNSA5OS4zNTQyTDI1OC4xIDk5LjM0OTVDMjU2LjAxMSA5Ny41MTkyIDI1Mi42MjUgOTcuNTE5MiAyNTAuNTM3IDk5LjM0OTRMMjUwLjUzMSA5OS4zNTQyQzI0OC40NDMgMTAxLjE4NCAyNDguNDQzIDEwNC4xNTIgMjUwLjUzMSAxMDUuOTgyTDI1MC41MzcgMTA1Ljk4N0MyNTIuNjI1IDEwNy44MTcgMjU2LjAxMSAxMDcuODE3IDI1OC4xIDEwNS45ODdaIiBmaWxsPSIjMjEyMTIxIiBzdHJva2U9IiMyMTIxMjEiIHN0cm9rZS1taXRlcmxpbWl0PSIxMCIvPgo8cGF0aCBkPSJNMjgwLjIxOSAxMzEuOTdMMjcxLjQyNyAxMzkuODg0SDE5OS43NjNWNTguMTI0M0MxOTkuNzYzIDU1Ljc5NTkgMjAwLjgxOSA1My41NjI5IDIwMi42OTcgNTEuOTE2NUMyMDQuNTc2IDUwLjI3MDIgMjA3LjEyNCA0OS4zNDUyIDIwOS43ODEgNDkuMzQ1MkgyNzAuMTk0QzI3Mi44NTEgNDkuMzQ1MiAyNzUuMzk5IDUwLjI3MDIgMjc3LjI3NyA1MS45MTY1QzI3OS4xNTYgNTMuNTYyOSAyODAuMjExIDU1Ljc5NTkgMjgwLjIxMSA1OC4xMjQzTDI4MC4yMTkgMTMxLjk3WiIgc3Ryb2tlPSIjMjEyMTIxIiBzdHJva2UtbWl0ZXJsaW1pdD0iMTAiLz4KPHBhdGggZD0iTTI1NC4zMiAxMzkuODc4SDE3My44NDlWNzIuNDc0NkMxNzMuODQ5IDcwLjE0NjMgMTc0LjkwNSA2Ny45MTMzIDE3Ni43ODMgNjYuMjY2OUMxNzguNjYyIDY0LjYyMDUgMTgxLjIxIDYzLjY5NTYgMTgzLjg2NyA2My42OTU2SDI0NC4yODdDMjQ2Ljk0NCA2My42OTU2IDI0OS40OTIgNjQuNjIwNSAyNTEuMzcxIDY2LjI2NjlDMjUzLjI0OSA2Ny45MTMzIDI1NC4zMDUgNzAuMTQ2MyAyNTQuMzA1IDcyLjQ3NDZMMjU0LjMyIDEzOS44NzhaIiBmaWxsPSJ3aGl0ZSIgc3Ryb2tlPSIjMjEyMTIxIiBzdHJva2UtbWl0ZXJsaW1pdD0iMTAiLz4KPHBhdGggZD0iTTE4Ny4xMDMgODQuOTM0MUgyMjQuNDQ1IiBzdHJva2U9IiMyMTIxMjEiIHN0cm9rZS1taXRlcmxpbWl0PSIxMCIvPgo8cGF0aCBkPSJNMTg3LjEwMyAxMDQuMjg5SDIzNy4yNTIiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0xODcuMTAzIDk0LjYxMTNIMjEzLjM3MiIgc3Ryb2tlPSIjMjEyMTIxIiBzdHJva2UtbWl0ZXJsaW1pdD0iMTAiLz4KPHBhdGggZD0iTTIzNy4yNDQgMTEyLjAxNEgxODcuMTAzVjEyNi4wMTNIMjM3LjI0NFYxMTIuMDE0WiIgZmlsbD0iIzAwODFGRiIvPgo8cGF0aCBkPSJNMSA3NS44MDRMMjEuMDY1NyA1OC4yMTIyTDQ3LjY5NjggNzUuODA0TDI3LjYzMSA5My4zODkyTDEgNzUuODA0WiIgZmlsbD0id2hpdGUiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiBzdHJva2UtbGluZWpvaW49InJvdW5kIi8+CjxwYXRoIGQ9Ik0xMTkuNjg0IDEwLjU4MjNWNzUuMTU1NiIgc3Ryb2tlPSIjMjEyMTIxIiBzdHJva2UtbWl0ZXJsaW1pdD0iMTAiIHN0cm9rZS1kYXNoYXJyYXk9IjQgNiIvPgo8cGF0aCBkPSJNMTE5LjY4NCA5Mi41MTExVjc1LjE1NTUiIHN0cm9rZT0id2hpdGUiIHN0cm9rZS1taXRlcmxpbWl0PSIxMCIgc3Ryb2tlLWRhc2hhcnJheT0iNCA2Ii8+CjxwYXRoIGQ9Ik0xNTIuNDY0IDEzOS44ODRMMTcyLjUzIDEyMi4yOTlIMTczLjg1NVYxMzkuODg0SDE1Mi40NjRaIiBmaWxsPSIjMjEyMTIxIi8+CjxwYXRoIGQ9Ik0yODAuMjE5IDEyMi4yOTlWMTMxLjk3TDI5MS41IDEyMi4yOTlIMjgwLjIxOVoiIGZpbGw9IiMyMTIxMjEiLz4KPHBhdGggZD0iTTExMy4xOTYgMTEuMzY1NUwxMjYuMTY1IDExLjM2NTVWLTMuMDUxNzZlLTA1TDExMy4xOTYgLTMuMDUxNzZlLTA1VjExLjM2NTVaIiBmaWxsPSIjMDBDQUZGIi8+CjxwYXRoIGQ9Ik0xOTkuODYzIDEyNi4wMTNDMTk5Ljg2MyAxMjYuMDEzIDIwMy4yNzcgMTE5LjAxIDIxNC45ODIgMTE5LjAxQzIyNi42ODcgMTE5LjAxIDIyNi44OTUgMTEyLjAxNCAyMjYuODk1IDExMi4wMTQiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0yMTguMTI2IDU4Ljk0MTRDMjIwLjE1MiA1OC45NDE0IDIyMS43OTQgNTcuNTAyMyAyMjEuNzk0IDU1LjcyNjlDMjIxLjc5NCA1My45NTE2IDIyMC4xNTIgNTIuNTEyNSAyMTguMTI2IDUyLjUxMjVDMjE2LjEgNTIuNTEyNSAyMTQuNDU4IDUzLjk1MTYgMjE0LjQ1OCA1NS43MjY5QzIxNC40NTggNTcuNTAyMyAyMTYuMSA1OC45NDE0IDIxOC4xMjYgNTguOTQxNFoiIGZpbGw9IiNGRjAwNzYiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+Cjwvc3ZnPgo=".toBase64ByteArray()
        )
      )
      response
    } else {
      PureResponse(HttpStatusCode.NotFound)
    }
  }

  runBlocking {
    dwebServer.createServer(httpHandler)
  }


  val dwebnmm = DWebViewTestNMM()

  val engine = DWebViewEngine(
//    CGRectMake(100.0, 100.0, 100.0, 200.0),
    platform.UIKit.UIScreen.mainScreen.bounds,
    dwebnmm,
    DWebViewOptions("http://dwebview.test.dweb/index.html"),
    WKWebViewConfiguration()
  )
  val dwebview = DWebView(engine)

  return ComposeUIViewController {
    Box(Modifier.fillMaxSize().background(Color.Cyan)) {
//      PreviewWindowDWebViewContent(engine, dwebview, onSizeChange)
      PreviewWindowTopBar(iosView, onSizeChange)
    }
  }
}

class DWebViewTestNMM() : NativeMicroModule("dwebview.test.dweb", "dwebview") {
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    TODO("Not yet implemented")
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }

}

@OptIn(ExperimentalForeignApi::class)
@Composable
fun PreviewWindowDWebViewContent(
  engine: DWebViewEngine,
  dWebView: DWebView,
  onSizeChange: (CGFloat, CGFloat) -> Unit
) {
  WindowPreviewer(modifier = Modifier.width(350.dp).height(500.dp), config = {
    state.title = "应用长长的标题的标题的标题～～"
    state.topBarContentColor = "#FF00FF"
    state.themeColor = "#Fd9F9F"
    state.iconUrl = "http://172.30.92.50:12207/m3-favicon-apple-touch.png"
    state.iconMaskable = true
    state.showMenuPanel = true
  }) { modifier ->
    Box() {
      onSizeChange(width.toDouble(), height.toDouble())
      Column {
        UIKitView(
          factory = {
            engine
          },
          modifier = Modifier.fillMaxWidth().height(200.dp),
          update = { view ->
            println("update:::: $view")
          })//    PreviewWindowTopBarContent(modifier)
        val scope = rememberCoroutineScope()
        ElevatedButton(onClick = {
          println("dwebview test start")
          engine.remoteMM.ioAsyncScope.launch {
            withMainContext {
              val channel = dWebView.createMessageChannel()
              channel.port2.onMessage {
                println("port2 on message: ${it.data}")
                if (it.data == "你好5") {
                  channel.port2.close()
                }
              }
              launch {
                var i = 0
                while (i++ < 5) {
                  println("postMessage $i")
                  channel.port1.postMessage(DWebMessage("你好$i"))
                  delay(100)
                  if (i > 3) {
                    channel.port2.start()
                  }
                }
                dWebView.postMessage("你好", listOf(channel.port1))
              }
            }
          }
          println("dwebview test end")
        }) {
          Text("创建channel")
        }
      }
    }
  }
}

@Composable
fun PreviewWindowTopBarContent(modifier: Modifier) {
  Box(
    modifier.background(Color.DarkGray)
  ) {
    val iconUrl by LocalWindowController.current.watchedState { iconUrl ?: "" }
    TextField(iconUrl, onValueChange = {}, modifier = Modifier.fillMaxSize())
  }
}
