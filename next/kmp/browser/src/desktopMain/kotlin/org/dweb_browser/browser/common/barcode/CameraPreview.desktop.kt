package org.dweb_browser.browser.common.barcode

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * 相机功能
 */
@Composable
actual fun CameraPreviewRender(
  openAlarmResult: (ImageBitmap) -> Unit,
  onBarcodeDetected: (QRCodeDecoderResult) -> Unit,
  maskView: @Composable (FlashLightSwitch, OpenAlbum) -> Unit
) {
  maskView({ it }, {})
}