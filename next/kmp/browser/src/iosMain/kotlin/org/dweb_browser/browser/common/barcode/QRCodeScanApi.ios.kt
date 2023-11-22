package org.dweb_browser.browser.common.barcode

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

@Composable
actual fun CameraPreviewView(
  openAlarmResult: (ImageBitmap) -> Unit,
  onBarcodeDetected: (QRCodeDecoderResult) -> Unit,
  maskView: @Composable (FlashLightSwitch, OpenAlbum) -> Unit
) {
  TODO("Not yet implemented")
}

actual fun beepAudio() {
  TODO("Not yet implemented")
}

actual fun decoderImage(
  imageBitmap: ImageBitmap, onSuccess: (QRCodeDecoderResult) -> Unit, onFailure: (Exception) -> Unit
) {
  TODO("Not yet implemented")
}

actual fun transformPoint(
  x: Int, y: Int, srcWidth: Int, srcHeight: Int, destWidth: Int, destHeight: Int, isFit: Boolean
): QRCodeDecoderResult.Point {
  TODO("Not yet implemented")
}

actual fun openDeepLink(data: String) {
  TODO("Not yet implemented")
}