package org.dweb_browser.browser.common.barcode

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.browser.util.regexDeepLink
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.encodeURIComponent
import org.dweb_browser.helper.isWebUrl
import org.dweb_browser.helper.platform.DeepLinkHook
import org.dweb_browser.helper.platform.NSDataHelper.toNSData
import org.dweb_browser.helper.platform.toByteArray
import org.dweb_browser.helper.toRect
import kotlin.math.abs
import platform.CoreImage.CIDetector
import platform.CoreImage.CIImage
import platform.CoreImage.CIQRCodeFeature
import platform.Foundation.NSData

actual fun beepAudio() {
  WARNING("Not yet implemented beepAudio")
}

@OptIn(ExperimentalForeignApi::class)
actual fun decoderImage(
  imageBitmap: ImageBitmap,
  onSuccess: (QRCodeDecoderResult) -> Unit,
  onFailure: (Exception) -> Unit,
) {
  val data = imageBitmap.toByteArray()?.toNSData() ?: return
  val ciImage = CIImage(data = data)
  val detector = CIDetector.detectorOfType("CIDetectorTypeQRCode", null, null)
  detector?.featuresInImage(image = ciImage, options = null)?.let { detectResult ->
    val listRect: MutableList<QRCodeDecoderResult.QRCode> = mutableListOf()
    (detectResult as List<CIQRCodeFeature>).map { barcode ->
      listRect.add(
        QRCodeDecoderResult.QRCode(
          org.dweb_browser.helper.PureRect(
            x = barcode.bounds.toRect().x + barcode.bounds.toRect().width / 2,
            y = barcode.bounds.toRect().y + barcode.bounds.toRect().height / 2,
          ), barcode.messageString()
        )
      )
      val qrCodeDecoderResult = QRCodeDecoderResult(imageBitmap, imageBitmap, listRect)
      onSuccess(qrCodeDecoderResult)
    }
  }
}

actual fun transformPoint(
  x: Int, y: Int, srcWidth: Int, srcHeight: Int, destWidth: Int, destHeight: Int, isAlarm: Boolean,
): QRCodeDecoderResult.Point {
  val widthRatio = destWidth * 1.0f / srcWidth
  val heightRatio = destHeight * 1.0f / srcHeight
  return if (isAlarm) {
    val ratio = widthRatio.coerceAtMost(heightRatio)
    val left = abs(srcWidth * ratio - destWidth) / 2
    val top = abs(srcHeight * ratio - destHeight) / 2
    QRCodeDecoderResult.Point(x * ratio + left, y * ratio + top)
  } else {
    val ratio = widthRatio.coerceAtLeast(heightRatio)
    val left = abs(srcWidth * ratio - destWidth) / 2
    val top = abs(srcHeight * ratio - destHeight) / 2
    QRCodeDecoderResult.Point(x * ratio - left, y * ratio - top)
  }
}

actual fun openDeepLink(data: String, showBackground: Boolean): Boolean {
  // 下面判断的是否是 DeepLink，如果不是的话，判断是否是
  val deepLink = data.regexDeepLink() ?: run {
    if (data.isWebUrl()) {
      "dweb://openinbrowser?url=${data.encodeURIComponent()}"
    } else {
      "dweb://search?q=${data.encodeURIComponent()}"
    }
  }
  DeepLinkHook.deepLinkHook.emitOnInit(deepLink)
  return true
}