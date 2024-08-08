package org.dweb_browser.browser.scan

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.browser.util.regexDeepLink
import org.dweb_browser.helper.isWebUrl
import org.dweb_browser.helper.platform.DeepLinkHook
import org.dweb_browser.helper.platform.NSDataHelper.toNSData
import org.dweb_browser.helper.toPoint
import org.dweb_browser.helper.toRect
import org.dweb_browser.helper.buildUrlString
import platform.CoreImage.CIDetector
import platform.CoreImage.CIDetectorTypeQRCode
import platform.CoreImage.CIImage
import platform.CoreImage.CIQRCodeFeature
import platform.Foundation.NSData
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle


actual class ScanningController actual constructor(mmScope: CoroutineScope) {

  actual fun stop() {}

  actual suspend fun recognize(data: ByteArray, rotation: Int): List<BarcodeResult> {
    if (data.isEmpty()) {

      return emptyList()
    }
    val ciImage = CIImage(data = data.toNSData())
    return process(ciImage)
  }

  suspend fun recognize(data: NSData) = process(CIImage(data = data))
  suspend fun recognize(image: CIImage) = process(image)

  @OptIn(ExperimentalForeignApi::class)
  private fun process(ciImage: CIImage): List<BarcodeResult> {
    val detector = CIDetector.detectorOfType(type = CIDetectorTypeQRCode, null, null)
    val detectResult =
      detector?.featuresInImage(image = ciImage, options = null) ?: return emptyList()
    @Suppress("UNCHECKED_CAST") return (detectResult as List<CIQRCodeFeature>).map {
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

  private var impact =
    UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)

  // 这里高度集成，没有调用HapticsNMM
  actual fun decodeHaptics() {
    impact.prepare()
    impact.impactOccurred()
  }
}


actual fun openDeepLink(data: String, showBackground: Boolean): Boolean {
  // 下面判断的是否是 DeepLink，如果不是的话，判断是否是
  val deepLink = data.regexDeepLink() ?: run {
    if (data.isWebUrl()) {
      buildUrlString("dweb://openinbrowser") {
        parameters["url"] = data
      }
    } else {
      buildUrlString("dweb://search") {
        parameters["q"] = data
      }
    }
  }
  DeepLinkHook.deepLinkHook.emitOnInit(deepLink)
  return true
}

