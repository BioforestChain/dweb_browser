package org.dweb_browser.browser.common.barcode

import androidx.compose.ui.graphics.ImageBitmap
import org.dweb_browser.helper.WARNING
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.SystemPermissionTask
import org.dweb_browser.sys.permission.ext.requestSystemPermission

class QRCodeScanController {
  private var qrCodeScanNMM: QRCodeScanNMM? = null

  fun init(qrCodeScanNMM: QRCodeScanNMM) {
    this.qrCodeScanNMM = qrCodeScanNMM
  }

  companion object {
    val controller by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { QRCodeScanController() }
  }

  /**
   * 获取权限
   */
  suspend fun checkPermission(): Boolean {
    return qrCodeScanNMM?.requestSystemPermission(
      SystemPermissionTask(
        SystemPermissionName.CAMERA,
        "进行二维码扫描",
      )
    ) == true
  }

  /**
   * 解析图片
   */
  suspend fun process(imageBitmap: ImageBitmap): List<String>? {
    WARNING("Not yet implement")
    return null
    /*return qrCodeScanNMM?.let {
      it.nativeFetch(
        PureRequest(
          URLBuilder("file://barcode-scanning.sys.dweb/process").buildUnsafeString(),
          method = IpcMethod.POST,
          body = IPureBody.Companion.from(imageBitmap)
        )
      ).json()
    }*/
  }
}