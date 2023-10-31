package org.dweb_browser.sys.permission

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.sys.window.core.helper.setFromManifest
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.onRenderer

val debugPermission = Debugger("permission")

class PermissionNMM : NativeMicroModule("permission.sys.dweb", "Permission Management") {
  init {
    short_name = "Permission";
    dweb_deeplinks = listOf("dweb://install")
    categories = listOf(
      MICRO_MODULE_CATEGORY.Application,
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Hub_Service
    )
    icons = listOf(
      ImageResource(src = "file:///sys/icons/$mmid.svg", type = "image/svg+xml")
    )

    /// 提供JsMicroModule的文件适配器
    /// 这个适配器不需要跟着bootstrap声明周期，只要存在JmmNMM模块，就能生效
    permissionAdaptersManager.append {

    }
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    permissionStdProtocol()
    onRenderer {
      getMainWindow().state.setFromManifest(this@PermissionNMM)
    }
  }

  override suspend fun _shutdown() {

  }
}