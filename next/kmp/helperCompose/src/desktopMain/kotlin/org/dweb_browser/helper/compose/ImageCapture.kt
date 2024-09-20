package org.dweb_browser.helper.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * 按照Chrome截图效果
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ImageCapture(imageBitmap: ImageBitmap, open: (ImageBitmap) -> Unit) {
  var movePoint by remember { mutableStateOf(Offset.Zero) }
  var startPoint by remember { mutableStateOf<Offset?>(null) }
  var endPoint by remember { mutableStateOf<Offset?>(null) }
  Canvas(modifier = Modifier.fillMaxSize()
    .onPointerEvent(PointerEventType.Move) { event ->
      movePoint = event.changes.first().position
    }
    .pointerInput(Unit) {
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
    }
  ) {
    if (startPoint != null && endPoint != null) {
      val rectStart = Offset(
        x = minOf(startPoint!!.x, endPoint!!.x),
        y = minOf(startPoint!!.y, endPoint!!.y)
      )
      val rectSize = Size(
        width = kotlin.math.abs(endPoint!!.x - startPoint!!.x),
        height = kotlin.math.abs(endPoint!!.y - startPoint!!.y)
      )
      // 绘制截屏的窗口
      drawRoundRect(
        color = Color.Red,
        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
        topLeft = rectStart,
        size = rectSize,
        style = Stroke(2f)
      )
    }
  }

  DrawMouseTips(movePoint) // 绘制鼠标跟随内容
}

/**
 * 绘制十字鼠标，每个像素点颜色需要跟背景有反差
 */
@Composable
private fun DrawMouseTips(movePoint: Offset) {
  Box(
    modifier = Modifier.size(30.dp)
      .offset { IntOffset(movePoint.x.toInt(), movePoint.y.toInt()) },
    contentAlignment = Alignment.Center
  ) {
    DrawWaterDrop(modifier = Modifier.size(30.dp)) {
      Icon(
        Icons.Filled.PhotoCamera,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error
      )
    }
  }
}

@Composable
private fun DrawWaterDrop(modifier: Modifier, content: @Composable () -> Unit = {}) {
  BoxWithConstraints(modifier = modifier) {
    val color = MaterialTheme.colorScheme.background
    Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
      val startX = 0f
      val startY = 0f
      val arcRadius = size.width / 2

      val path = Path().apply {
        // 移动到顶部
        moveTo(x = startX, y = startY)
        // 画直线
        lineTo(x = startX, y = startY + arcRadius)
        // 画圆弧
        arcTo(
          rect = Rect(
            Offset(startX, startY), // 外置矩形的位置和大小
            Size(arcRadius * 2, arcRadius * 2)
          ),
          startAngleDegrees = 180f,
          sweepAngleDegrees = -270f,
          forceMoveTo = false
        )

        close() // 闭合路径
      }
      // 填充水滴
      drawPath(
        path = path,
        color = color
      )
    }
    Box(
      modifier = Modifier.fillMaxSize().padding(8.dp).clip(CircleShape),
      contentAlignment = Alignment.Center
    ) {
      content()
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