package org.dweb_browser.browser.common.barcode

import androidx.compose.ui.graphics.ImageBitmap
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource
import org.dweb_browser.helper.falseAlso
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.SystemPermissionTask
import org.dweb_browser.sys.permission.ext.requestSystemPermission
import org.dweb_browser.sys.toast.ext.showToast

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
  suspend fun requestPermission(): Boolean {
    return qrCodeScanNMM?.requestSystemPermission(
      SystemPermissionTask(
        SystemPermissionName.CAMERA,
        title = QRCodeI18nResource.permission_tip_camera_title.text,
        description = QRCodeI18nResource.permission_tip_camera_message.text
      )
    )?.falseAlso {
      qrCodeScanNMM?.showToast(QRCodeI18nResource.permission_denied.text)
    } == true
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

object QRCodeI18nResource {
  val permission_tip_camera_title = SimpleI18nResource(
    Language.ZH to "相机权限使用说明",
    Language.EN to "Camera Permission Instructions"
  )
  val permission_tip_camera_message = SimpleI18nResource(
    Language.ZH to "DwebBrowser正在向您获取“相机”权限，同意后，将用于为您提供扫描二维码服务",
    Language.EN to "DwebBrowser is asking you for \"Camera\" permissions, and if you agree, it will be used to provide you with scanning QR code services"
  )

  val permission_denied = SimpleI18nResource(
    Language.ZH to "权限被拒绝，无法提供扫码服务",
    Language.EN to "The permission is denied, and the code scanning service cannot be provided"
  )
}