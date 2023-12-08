package org.dweb_browser.browser.common.barcode

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule

class QRCodeScanNMM : NativeMicroModule("qrcode-scan.browser.dweb", "QRCode-Scan") {
  init {
    short_name = "二维码扫描"
    categories = listOf(
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Hub_Service
    )
    QRCodeScanController.controller.init(this)
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}