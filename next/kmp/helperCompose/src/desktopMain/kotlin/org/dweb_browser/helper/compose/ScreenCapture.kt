package org.dweb_browser.helper.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ScreenCapture(imageBitmap: ImageBitmap, open: (ImageBitmap) -> Unit) {
  var movePoint by remember { mutableStateOf<Offset?>(null) }
  var startPoint by remember { mutableStateOf<Offset?>(null) }
  var endPoint by remember { mutableStateOf<Offset?>(null) }
  Canvas(
    modifier = Modifier.fillMaxSize().onPointerEvent(PointerEventType.Move) { event ->
      movePoint = event.changes.first().position
    }.pointerInput(Unit) {
      detectDragGestures(
        onDragStart = { offset ->
          startPoint = offset
          endPoint = null
        },
        onDrag = { change, _ ->
          endPoint = change.position
        },
        onDragEnd = {
          // 先截图，然后发送图片信息给扫码模块，并关闭当前窗口
          open(imageBitmap.crop(startPoint!!, endPoint!!))
          startPoint = null
          endPoint = null
        }
      )
    }) {
    if (startPoint != null && endPoint != null) {
      val rectStart = Offset(
        x = minOf(startPoint!!.x, endPoint!!.x),
        y = minOf(startPoint!!.y, endPoint!!.y)
      )
      val rectSize = androidx.compose.ui.geometry.Size(
        width = kotlin.math.abs(endPoint!!.x - startPoint!!.x),
        height = kotlin.math.abs(endPoint!!.y - startPoint!!.y)
      )
      // 绘制截屏的窗口
      drawRect(
        color = Color.Red,
        topLeft = rectStart,
        size = rectSize,
        style = Stroke(2f)
      )
    } else {
      // 绘制十字线
      movePoint?.let { offset ->
        drawLine(
          color = Color.Red,
          start = Offset(offset.x, 0f),
          end = Offset(offset.x, size.height)
        )
        drawLine(
          color = Color.Red,
          start = Offset(0f, offset.y),
          end = Offset(size.width, offset.y)
        )
      }
    }
  }
}

private fun ImageBitmap.crop(startPoint: Offset, endPoint: Offset): ImageBitmap {
  val x = minOf(startPoint.x, endPoint.x).toInt()
  val y = minOf(startPoint.y, endPoint.y).toInt()
  val width = kotlin.math.abs(endPoint.x - startPoint.x).toInt()
  val height = kotlin.math.abs(endPoint.y - startPoint.y).toInt()
  val bufferedImage = this.toAwtImage().getSubimage(x, y, width, height)
  return bufferedImage.toComposeImageBitmap()
}