package org.dweb_browser.browser.common.barcode

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import org.dweb_browser.helper.WARNING

@Composable
actual fun CameraPreviewView(
  openAlarmResult: (ImageBitmap) -> Unit,
  onBarcodeDetected: (QRCodeDecoderResult) -> Unit,
  maskView: @Composable (FlashLightSwitch, OpenAlbum) -> Unit
) {
  WARNING("Not yet implemented CameraPreviewView")
}

actual fun beepAudio() {
  WARNING("Not yet implemented beepAudio")
}

actual fun decoderImage(
  imageBitmap: ImageBitmap, onSuccess: (QRCodeDecoderResult) -> Unit, onFailure: (Exception) -> Unit
) {
  WARNING("Not yet implemented decoderImage")
}

actual fun transformPoint(
  x: Int, y: Int, srcWidth: Int, srcHeight: Int, destWidth: Int, destHeight: Int, isFit: Boolean
): QRCodeDecoderResult.Point {
  WARNING("Not yet implemented transformPoint")
  TODO("Not yet implemented")
}

actual fun openDeepLink(data: String) {
  WARNING("Not yet implemented openDeepLink")
}