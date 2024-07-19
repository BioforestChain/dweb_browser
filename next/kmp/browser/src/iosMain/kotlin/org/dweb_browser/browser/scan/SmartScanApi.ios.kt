package org.dweb_browser.browser.scan

import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.browser.util.regexDeepLink
import org.dweb_browser.helper.encodeURIComponent
import org.dweb_browser.helper.isWebUrl
import org.dweb_browser.helper.platform.DeepLinkHook
import org.dweb_browser.helper.toPoint
import org.dweb_browser.helper.toRect
import platform.CoreImage.CIDetector
import platform.CoreImage.CIImage
import platform.CoreImage.CIQRCodeFeature
import platform.Foundation.NSData


actual class ScanningManager actual constructor() {

  actual fun stop() {}

  @OptIn(ExperimentalForeignApi::class)
  actual suspend fun recognize(data: Any, rotation: Int): List<BarcodeResult> {
    if (data is NSData) {
      val ciImage = CIImage(data = data)
      val detector = CIDetector.detectorOfType("CIDetectorTypeQRCode", null, null)
      val detectResult =
        detector?.featuresInImage(image = ciImage, options = null) ?: return emptyList()
      @Suppress("UNCHECKED_CAST")
      return (detectResult as List<CIQRCodeFeature>).map {
        BarcodeResult(
          it.messageString ?: "",
          boundingBox = it.bounds.toRect(),
          topLeft = it.topLeft.toPoint(),
          topRight = it.topRight.toPoint(),
          bottomLeft = it.bottomLeft.toPoint(),
          bottomRight = it.bottomRight.toPoint(),
        )
      }
    }
    return emptyList()
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

