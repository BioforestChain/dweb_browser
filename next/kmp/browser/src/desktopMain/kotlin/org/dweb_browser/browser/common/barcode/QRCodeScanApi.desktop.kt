package org.dweb_browser.browser.common.barcode

import androidx.compose.ui.graphics.ImageBitmap
import org.dweb_browser.helper.WARNING

/**
 * 振动，响铃
 */
actual fun beepAudio() {
  TODO("Not yet implemented beepAudio")
}

actual fun decoderImage(
  imageBitmap: ImageBitmap, onSuccess: (QRCodeDecoderResult) -> Unit, onFailure: (Exception) -> Unit
) {
  TODO("Not yet implemented decoderImage")
}

/**
 * 计算二维码的位置
 */
actual fun transformPoint(
  x: Int,
  y: Int,
  srcWidth: Int,
  srcHeight: Int,
  destWidth: Int,
  destHeight: Int,
  isFit: Boolean
): QRCodeDecoderResult.Point {
  WARNING("Not yet implemented transformPoint")
  return QRCodeDecoderResult.Point(0f, 0f)
}

/**
 * 打开扫码的结果
 */
actual fun openDeepLink(data: String, showBackground: Boolean): Boolean {
  WARNING("Not yet implemented openDeepLink")
  return false
}