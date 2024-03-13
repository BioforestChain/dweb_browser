package org.dweb_browser.helper.capturable

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.skia.Picture
import org.jetbrains.skia.PictureRecorder
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface

internal actual suspend fun getCurrentContentAsImageBitmap(
  node: CapturableModifierNode,
  config: CaptureController.CaptureConfig
): ImageBitmap {
  val pictureDeferred = CompletableDeferred<Picture>()
  val off = node.drawDelegate {
    val pictureRecorder = PictureRecorder()
    onDrawWithContent {
      val pictureCanvas = pictureRecorder.beginRecording(Rect(0f, 0f, size.width, size.height))
      draw(this, this.layoutDirection, pictureCanvas.asComposeCanvas(), this.size) {
        this@onDrawWithContent.drawContent()
      }
      val picture = pictureRecorder.finishRecordingAsPicture()
      drawIntoCanvas {
        it.nativeCanvas.drawPicture(picture)
        pictureDeferred.complete(picture)
      }
    }
  }
  val picture = pictureDeferred.await()
  off()

  /// 将 picture 光栅化成 imageBitmap
  val surface =
    Surface.makeRasterN32Premul(picture.cullRect.width.toInt(), picture.cullRect.height.toInt())
  surface.canvas.drawPicture(picture)
  return surface.makeImageSnapshot().toComposeImageBitmap()
}
