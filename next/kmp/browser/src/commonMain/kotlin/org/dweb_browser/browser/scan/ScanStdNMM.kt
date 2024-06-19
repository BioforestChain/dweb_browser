package org.dweb_browser.browser.scan

import io.ktor.http.HttpStatusCode
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.common.barcode.openDeepLink
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.ResponseException
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.DisplayMode
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.ext.requestSystemPermission
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.onRenderer

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
  }

  inner class ScanStdRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    override suspend fun _bootstrap() {
      val scanStdController = ScanStdController(this)
      onRenderer {
        scanStdController.renderScanWindow(wid) // 窗口已打开，默认还是直接渲染，再请求权限
        if (requestSystemPermission(
            title = BrowserI18nResource.QRCode.permission_tip_camera_title.text,
            description = BrowserI18nResource.QRCode.permission_tip_camera_message.text,
            name = SystemPermissionName.CAMERA
          )
        ) {
          val result = scanStdController.tryShowScanWindow()
          if (result?.isNotEmpty() == true) {
            openDeepLink(result)
          }
        } else {
          // 权限请求失败，直接退出
          getMainWindow().closeRoot()
        }
      }

      routes(
        /**
         * 打开扫码界面，并返回扫码字符串
         */
        "/open" bind PureMethod.GET by defineStringResponse {
          debugSCAN("open", request.href)
          scanStdController.tryShowScanWindow() ?: throw ResponseException(
            code = HttpStatusCode.NoContent,
            message = BrowserI18nResource.QRCode.permission_tip_camera_message.text
          )
        },
      )
    }

    override suspend fun _shutdown() {
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext): Runtime {
    return ScanStdRuntime(bootstrapContext)
  }
}