package org.dweb_browser.browser.barcode

import org.dweb_browser.helper.PromiseOut

class ScanningController {
  companion object {
    val controller = ScanningController()
  }

  private var scanResult = PromiseOut<String>()
  suspend fun waitScanResult() = scanResult.waitPromise()

  var scanData: String? = null
    set(value) {
      if (field == value) return
      field = value
      if (value == null) {
        scanResult = PromiseOut()
      } else {
        scanResult.resolve(value)
      }
    }
}