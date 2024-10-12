package org.dweb_browser.browser.scan

import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.CancellationException
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.core.help.types.DwebPermission
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.DisplayMode
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.listen
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.SystemPermissionTask
import org.dweb_browser.sys.permission.ext.requestSystemPermissions
import org.dweb_browser.sys.window.ext.onRenderer
import org.jetbrains.compose.resources.ExperimentalResourceApi

val debugSCAN = Debugger("scan.browser")

class SmartScanNMM : NativeMicroModule("scan.browser.dweb", "Smart Scan") {
  init {
    short_name = BrowserI18nResource.QRCode.short_name.text
    categories = listOf(
      MICRO_MODULE_CATEGORY.Application,
    )
    dweb_protocols = listOf("barcode-scanning.sys.dweb")
    icons = listOf(
      ImageResource(src = "file:///sys/browser-icons/$mmid.svg", type = "image/svg+xml")
    )
    display = DisplayMode.Fullscreen
    dweb_permissions = listOf(
      DwebPermission(
        pid = "$mmid/open",
        routes = listOf("file://$mmid/open"),
        title = BrowserI18nResource.QRCode.permission_tip_camera_title.text,
        description = BrowserI18nResource.QRCode.permission_tip_camera_message.text
      )
    )
  }

  inner class ScanRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun _bootstrap() {
      val scanningController = ScanningController(mmScope)
      // 实现barcodeScanning协议
      barcodeScanning(scanningController)
      val scanController = SmartScanController(this, scanningController)
      onRenderer {
        val isPermission = requestSystemPermission()
        if (isPermission) {
          val controller = scanController.getWindowController()
          // 渲染扫码页面，在桌面端作用为选择图片文件
          controller.show()
          try {
            val result = scanController.saningResult.await()
            openDeepLink(result)
          } catch (e: CancellationException) {
            debugSCAN("onRenderer", "Deferred was cancelled=> ${e.message}")
          }
        }
      }

      routes(
        /**
         * 打开扫码界面，并返回扫码字符串
         */
        "/open" bind PureMethod.GET by defineStringResponse {
          val isPermission = requestSystemPermission()
          debugSCAN("scan/open", "${request.href} isPermission=>$isPermission")
          if (isPermission) {
            // 创建对应的控制器
            val controller = scanController.getWindowController()
            controller.show()
          } else {
            throwException(
              HttpStatusCode.Unauthorized,
              BrowserI18nResource.QRCode.permission_denied.text
            )
          }
          try {
            scanController.saningResult.await()
          } catch (e: Exception) {
            throwException(
              HttpStatusCode.InternalServerError,
              e.message
            )
          }
        },
        "/parseImage" bind PureMethod.POST by defineEmptyResponse {
          val controller = scanController.getWindowController()
          controller.show()
          scanController.albumImageFlow.tryEmit(request.body.toPureBinary())
        }
      )

      // 获取ShortcutManage发起的open操作
      ipcConnectedProducer.consumer("for-shortcut-scan").listen { connectEvent ->
        val (ipc) = connectEvent.consume()
        ipc.onEvent("shortcut-open").collect {
          debugSCAN("shortcut-open", "open scan => from=${ipc.remote.mmid}, name=${it.data.name}")
          if (it.data.name == "shortcut-open") {
            nativeFetch(buildUrlString("file://desk.browser.dweb/openAppOrActivate") {
              parameters["app_id"] = mmid
            })
          }
        }
      }
    }

    private suspend fun requestSystemPermission(): Boolean {
      val permission = requestSystemPermissions(
        SystemPermissionTask(
          name = SystemPermissionName.CAMERA,
          title = BrowserI18nResource.QRCode.permission_tip_camera_title.text,
          description = BrowserI18nResource.QRCode.permission_tip_camera_message.text
        )
      )
      return permission.filterValues { it != AuthorizationStatus.GRANTED }.isEmpty()
    }

    override suspend fun _shutdown() {
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext): Runtime {
    return ScanRuntime(bootstrapContext)
  }
}