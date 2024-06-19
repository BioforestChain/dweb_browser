package org.dweb_browser.browser.common.barcode

import androidx.compose.ui.graphics.ImageBitmap
import org.dweb_browser.browser.util.regexDeepLink
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.encodeURIComponent
import org.dweb_browser.helper.isWebUrl
import org.dweb_browser.helper.platform.DeepLinkHook

actual fun beepAudio() {
  WARNING("Not yet implemented beepAudio")
}

actual fun decoderImage(
  imageBitmap: ImageBitmap,
  onSuccess: (QRCodeDecoderResult) -> Unit,
  onFailure: (Exception) -> Unit,
) {
  WARNING("Not yet implemented decoderImage")
}

actual fun transformPoint(
  x: Int, y: Int, srcWidth: Int, srcHeight: Int, destWidth: Int, destHeight: Int, isAlarm: Boolean,
): QRCodeDecoderResult.Point {
  WARNING("Not yet implemented transformPoint")
  return QRCodeDecoderResult.Point(0f, 0f)
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