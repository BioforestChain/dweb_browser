package org.dweb_browser.browser.common.barcode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.imageio.ImageIO

/**
 * 相机功能
 */
@Composable
actual fun CameraPreviewRender(
  openAlarmResult: (ImageBitmap) -> Unit,
  onBarcodeDetected: (QRCodeDecoderResult) -> Unit,
  maskView: @Composable (FlashLightSwitch, OpenAlbum) -> Unit,
  onCancel: (String) -> Unit,
) {
  // maskView({ it }, {})
  // 目前考虑到电脑端没有摄像头处理二维码，这边考虑直接使用本地图片打开的方式
  LaunchedEffect(Unit) {
    val imageBitmap = chooserQRCodeImage()
    imageBitmap?.let { openAlarmResult(imageBitmap) } ?: onCancel("Cancel")
  }
}

fun chooserQRCodeImage(): ImageBitmap? {
  val dialog = FileDialog(Frame(), "选择一张二维码图片", FileDialog.LOAD)
  dialog.isVisible = true

  val fileName = dialog.file
  val directory = dialog.directory

  println("openFile() => fileName=$fileName, directory=$directory")
  dialog.isVisible = false
  return if (fileName != null && directory != null) {
    val file = File(directory, fileName)
    try {
      val bufferImage = ImageIO.read(file)
      bufferImage.toComposeImageBitmap()
    } catch (e: Exception) {
      null
    }
  } else null
}