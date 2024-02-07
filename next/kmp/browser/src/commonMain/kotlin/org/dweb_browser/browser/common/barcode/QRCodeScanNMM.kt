package org.dweb_browser.browser.common.barcode

import org.dweb_browser.core.help.types.DwebPermission
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.pure.http.PureMethod

class QRCodeScanNMM : NativeMicroModule("qrcode-scan.browser.dweb", "QRCode-Scan") {
  init {
    short_name = "二维码扫描"
    categories = listOf(
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Hub_Service
    )
    dweb_permissions = listOf(
      DwebPermission(
        pid = "$mmid/camera",
        routes = listOf("file://$mmid/scan"),
        title = "申请相机权限",
      )
    )
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    QRCodeScanController.controller.init(this@QRCodeScanNMM)
    routes(
      "/scan" bind PureMethod.GET by defineEmptyResponse {

      }
    ).cors()
  }

  override suspend fun _shutdown() {
  }
}