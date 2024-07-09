package org.dweb_browser.browser.scan

import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.CancellationException
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.core.help.types.DwebPermission
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.DisplayMode
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.SystemPermissionTask
import org.dweb_browser.sys.permission.ext.requestSystemPermissions
import org.dweb_browser.sys.window.ext.onRenderer
import org.dweb_browser.sys.window.ext.openMainWindow

val debugSCAN = Debugger("scan.std")

class ScanStdNMM : NativeMicroModule("scan.std.dweb", "QRCode Scan") {
  init {
    short_name = BrowserI18nResource.QRCode.short_name.text
    categories = listOf(
      MICRO_MODULE_CATEGORY.Application,
    )
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

  inner class ScanStdRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    override suspend fun _bootstrap() {
      val scanController = ScanStdController(this)
      onRenderer {
        val isPermission = requestSystemPermission()
        if (isPermission) {
          // 渲染扫码页面，在桌面端作用为选择图片文件
          scanController.getWindowController().show()
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
          scanController.saningResult.await()
        },
      )
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
    return ScanStdRuntime(bootstrapContext)
  }
}