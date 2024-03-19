package org.dweb_browser.browser.common.barcode

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap


/**
 * 相机功能
 */
@Composable
actual fun CameraPreviewView(
  openAlarmResult: (ImageBitmap) -> Unit,
  onBarcodeDetected: (QRCodeDecoderResult) -> Unit,
  maskView: @Composable (FlashLightSwitch, OpenAlbum) -> Unit
) {
  maskView({ it }, {})
}

/**
 * 振动，响铃
 */
actual fun beepAudio() {
  TODO("Not yet implemented beepAudio")
}
actual fun decoderImage(
  imageBitmap: ImageBitmap, onSuccess: (QRCodeDecoderResult) -> Unit, onFailure: (Exception) -> Unit
){
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
  TODO("Not yet implemented transformPoint")
}

/**
 * 打开扫码的结果
 */
actual fun openDeepLink(data: String) {
  TODO("Not yet implemented openDeepLink")
}