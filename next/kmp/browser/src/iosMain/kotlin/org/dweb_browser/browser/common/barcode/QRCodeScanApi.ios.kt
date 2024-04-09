package org.dweb_browser.browser.common.barcode

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import org.dweb_browser.helper.WARNING

actual fun beepAudio() {
  WARNING("Not yet implemented beepAudio")
}

actual fun decoderImage(
  imageBitmap: ImageBitmap, onSuccess: (QRCodeDecoderResult) -> Unit, onFailure: (Exception) -> Unit
) {
  WARNING("Not yet implemented decoderImage")
}

actual fun transformPoint(
  x: Int, y: Int, srcWidth: Int, srcHeight: Int, destWidth: Int, destHeight: Int, isAlarm: Boolean
): QRCodeDecoderResult.Point {
  WARNING("Not yet implemented transformPoint")
  return QRCodeDecoderResult.Point(0f, 0f)
}

actual fun openDeepLink(data: String, showBackground: Boolean): Boolean {
  WARNING("Not yet implemented openDeepLink")
  return false
}