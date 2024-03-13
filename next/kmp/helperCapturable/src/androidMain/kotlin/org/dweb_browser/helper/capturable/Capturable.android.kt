package org.dweb_browser.helper.capturable

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Picture
import android.os.Build
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withContext
import org.dweb_browser.helper.defaultAsyncExceptionHandler

internal actual suspend fun getCurrentContentAsImageBitmap(
  node: CapturableModifierNode,
  config: CaptureController.CaptureConfig
): ImageBitmap {
  val picture = node.drawCanvasIntoPicture()
  val bitmap = withContext(defaultAsyncExceptionHandler) {
    picture.asBitmap(config.bitmapConfig)
  }
  return bitmap.asImageBitmap()
}

/**
 * Draws the current content into the provided [picture]
 */
private suspend fun CapturableModifierNode.drawCanvasIntoPicture() = Picture().also { picture ->
  // CompletableDeferred to wait until picture is drawn from the Canvas content
  val pictureDrawn = CompletableDeferred<Unit>()

  // Delegate the task to draw the content into the picture
  val offDelegate = drawDelegate {
    val width = this.size.width.toInt()
    val height = this.size.height.toInt()

    onDrawWithContent {
      val pictureCanvas = Canvas(picture.beginRecording(width, height))

      draw(this, this.layoutDirection, pictureCanvas, this.size) {
        this@onDrawWithContent.drawContent()
      }
      picture.endRecording()

      drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawPicture(picture)

        // Notify that picture is drawn
        pictureDrawn.complete(Unit)
      }
    }
  }
  // Wait until picture is drawn
  pictureDrawn.await()

  // As task is accomplished, remove the delegation of node to prevent draw operations on UI
  // updates or recompositions.
  offDelegate()
}

/**
 * Creates a [Bitmap] from a [Picture] with provided [config]
 */
@SuppressLint("ObsoleteSdkInt")
private fun Picture.asBitmap(config: Bitmap.Config): Bitmap {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    Bitmap.createBitmap(this@asBitmap)
  } else {
    val bitmap = Bitmap.createBitmap(
      /* width = */
      this@asBitmap.width,
      /* height = */
      this@asBitmap.height,
      /* config = */
      config
    )
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    canvas.drawPicture(this@asBitmap)
    bitmap
  }
}

var CaptureController.CaptureConfig.bitmapConfig: Bitmap.Config
  get() {
    return source["bitmap.config"]?.let { if (it is Bitmap.Config) it else null }
      ?: Bitmap.Config.ARGB_8888
  }
  set(value) {
    source["bitmap.config"] = value
  }
