package org.dweb_browser.browser.common.barcode

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import org.dweb_browser.helper.WARNING

@Composable
actual fun CameraPreviewRender(
  openAlarmResult: (ImageBitmap) -> Unit,
  onBarcodeDetected: (QRCodeDecoderResult) -> Unit,
  maskView: @Composable (FlashLightSwitch, OpenAlbum) -> Unit,
  onCancel: (String) -> Unit,
) {
  WARNING("Not yet implemented CameraPreviewView")
}