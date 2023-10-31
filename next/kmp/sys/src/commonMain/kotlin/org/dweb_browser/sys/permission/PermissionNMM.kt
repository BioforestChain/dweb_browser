package org.dweb_browser.sys.permission

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.permission.permissionStdProtocol
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.sys.window.core.helper.setFromManifest
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.onRenderer


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