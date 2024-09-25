package org.dweb_browser.helper.compose

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
      // 绘制一个带有阴影的矩形
      val topLeftX = minOf(startPoint!!.x, endPoint!!.x)
      val topLeftY = minOf(startPoint!!.y, endPoint!!.y)
      val rectWidth = kotlin.math.abs(endPoint!!.x - startPoint!!.x)
      val rectHeight = kotlin.math.abs(endPoint!!.y - startPoint!!.y)

      // 绘制截屏的窗口
      drawCustomRoundRect(
        color = Color.Red,
        topLeft = Offset(topLeftX, topLeftY),
        size = Size(rectWidth, rectHeight),
        radius = 32f,
        mouseCorner = if (startPoint!!.x > endPoint!!.x) {
          if (startPoint!!.y > endPoint!!.y) Alignment.TopStart else Alignment.BottomStart
        } else {
          if (startPoint!!.y > endPoint!!.y) Alignment.TopEnd else Alignment.BottomEnd
        }
      )
      // drawRoundShadow2(topLeft = Offset(topLeftX, topLeftY), size = Size(rectWidth, rectHeight))
    }
  }
  DrawMouseTips(movePoint) // 绘制鼠标跟随内容
}

/**
 * 绘制一个指定角度为直角的矩形
 * @param radius 表示弧度的半径
 * @param mouseCorner 表示哪个角保持直角
 */
private fun DrawScope.drawCustomRoundRect(
  color: Color,
  topLeft: Offset,
  size: Size,
  radius: Float,
  mouseCorner: Alignment
) {
  val radiusWidth = if (size.width <= 2 * radius) size.width / 2 else radius
  val radiusHeight = if (size.height <= 2 * radius) size.height / 2 else radius
  val path = Path().apply {
    moveTo(topLeft.x + radiusWidth, topLeft.y)
    // 如果右上角是直角的时候
    if (mouseCorner == Alignment.TopEnd) {
      lineTo(topLeft.x + size.width, topLeft.y)
      lineTo(topLeft.x + size.width, topLeft.y + radiusHeight)
    } else {
      lineTo(topLeft.x + size.width - radiusWidth, topLeft.y)
      arcTo(
        rect = Rect(
          topLeft = Offset(topLeft.x + size.width - radiusWidth, topLeft.y),
          bottomRight = Offset(topLeft.x + size.width, topLeft.y + radiusHeight)
        ),
        startAngleDegrees = -90f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false
      )
    }
    // 如果右下角是直角
    if (mouseCorner == Alignment.BottomEnd) {
      lineTo(topLeft.x + size.width, topLeft.y + size.height)
      lineTo(topLeft.x + size.width - radiusWidth, topLeft.y + size.height)
    } else {
      lineTo(topLeft.x + size.width, topLeft.y + size.height - radiusHeight)
      arcTo(
        rect = Rect(
          topLeft = Offset(
            topLeft.x + size.width - radiusWidth,
            topLeft.y + size.height - radiusHeight
          ),
          bottomRight = Offset(topLeft.x + size.width, topLeft.y + size.height)
        ),
        startAngleDegrees = 0f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false
      )
    }
    // 如果左下角是直角
    if (mouseCorner == Alignment.BottomStart) {
      lineTo(topLeft.x, topLeft.y + size.height)
      lineTo(topLeft.x, topLeft.y + size.height - radiusHeight)
    } else {
      lineTo(topLeft.x + radiusWidth, topLeft.y + size.height)
      arcTo(
        rect = Rect(
          topLeft = Offset(topLeft.x, topLeft.y + size.height - radiusHeight),
          bottomRight = Offset(topLeft.x + radiusWidth, topLeft.y + size.height)
        ),
        startAngleDegrees = 90f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false
      )
    }
    // 如果左上角是直角
    if (mouseCorner == Alignment.TopStart) {
      lineTo(topLeft.x, topLeft.y)
    } else {
      lineTo(topLeft.x, topLeft.y + radiusHeight)
      arcTo(
        rect = Rect(
          topLeft = Offset(topLeft.x, topLeft.y),
          bottomRight = Offset(topLeft.x + radiusWidth, topLeft.y + radiusHeight)
        ),
        startAngleDegrees = 180f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false
      )
    }
    close()
  }
  drawPath(path = path, color = color, style = Stroke(4f))
}

/**
 * 绘制阴影1 => 绘制一个椭圆形的阴影，但是发现这个阴影没办法将中间进行矩形抠图
 */
private fun DrawScope.drawRoundShadow(topLeft: Offset, size: Size) {
  val shadow = maxOf(minOf(size.width, size.height), 60f)
  val ellipseTopLeft = Offset(topLeft.x - shadow / 2, topLeft.y - shadow / 2)
  val ellipseSize = Size(size.width + shadow, size.height + shadow)
  // 如果 宽高比小于 1/2 或者大于2的话，只需要显示长的那边
  val ratio = 1.0f * size.width / size.height
  val centerOffset = Offset(topLeft.x + size.width / 2, topLeft.y + size.height / 2)
  val radius = when {
    ratio < 1.0f / 2 -> {
      ellipseSize.width / 2
    }

    ratio > 2.0f -> {
      ellipseSize.height / 2
    }

    else -> {
      maxOf(ellipseSize.width, ellipseSize.height) / 2
    }
  }

  val horizontalBrush = Brush.radialGradient(
    colors = listOf(Color.DarkGray.copy(0.4f), Color.Transparent),
    center = centerOffset,
    radius = radius
  )
  drawOval(
    brush = horizontalBrush,
    topLeft = ellipseTopLeft,
    size = ellipseSize
  )
}

/**
 * 绘制阴影2 => 采用上下左右各自进行绘制，但是发现拐角和汇合的地方有问题
 */
private fun DrawScope.drawRoundShadow2(topLeft: Offset, size: Size) {
  val shadowWidth = maxOf(size.width, 40f) + size.width // 横向
  val shadowHeight = maxOf(size.height, 40f) + size.height // 纵向

//  val startTopLeftOffset = Offset(
//    x = topLeft.x - shadowHeight / 2,
//    y = topLeft.y - (shadowHeight - size.height) / 2
//  )
//  val startSize = Size(width = shadowHeight, height = shadowHeight)
//  val startCenterOffset = Offset(topLeft.x, topLeft.y + size.height / 2)
//  val startBrush = Brush.radialGradient(
//    colors = listOf(Color.DarkGray.copy(0.4f), Color.Transparent),
//    center = startCenterOffset,
//    radius = shadowHeight / 2
//  )
//  drawArc(
//    brush = startBrush,
//    startAngle = 90f,
//    sweepAngle = 180f,
//    useCenter = true,
//    topLeft = startTopLeftOffset,
//    size = startSize
//  )

  // 左边
  drawShadow(
    startAngle = 90f,
    sweepAngle = 180f,
    topLeftX = topLeft.x - (shadowHeight - 40f) / 2,
    topLeftY = topLeft.y - (shadowHeight - size.height) / 2,
    width = shadowHeight - 40f,
    height = shadowHeight
  )

  // 上边
  drawShadow(
    startAngle = 180f,
    sweepAngle = 180f,
    topLeftX = topLeft.x - (shadowWidth - size.width) / 2,
    topLeftY = topLeft.y - (shadowWidth - 40f) / 2,
    width = shadowWidth,
    height = shadowWidth - 40f
  )

  // 右边
  drawShadow(
    startAngle = -90f,
    sweepAngle = 180f,
    topLeftX = topLeft.x + size.width - (shadowHeight - 40f) / 2,
    topLeftY = topLeft.y - (shadowHeight - size.height) / 2,
    width = shadowHeight - 40f,
    height = shadowHeight
  )

  // 下边
  drawShadow(
    startAngle = 0f,
    sweepAngle = 180f,
    topLeftX = topLeft.x - (shadowWidth - size.width) / 2,
    topLeftY = topLeft.y + size.height - (shadowWidth - 40f) / 2,
    width = shadowWidth,
    height = shadowWidth - 40f
  )
}

private fun DrawScope.drawShadow(
  startAngle: Float,
  sweepAngle: Float,
  topLeftX: Float,
  topLeftY: Float,
  width: Float,
  height: Float,
) {
  val topLeftOffset = Offset(x = topLeftX, y = topLeftY)
  val size = Size(width = width, height = height)
  val centerOffset = Offset(topLeftX + width / 2, topLeftY + height / 2)
  val brush = Brush.radialGradient(
    colors = listOf(Color.DarkGray.copy(0.4f), Color.Transparent),
    center = centerOffset,
    radius = minOf(width, height) / 2
  )
  drawArc(
    brush = brush,
    startAngle = startAngle,
    sweepAngle = sweepAngle,
    useCenter = true,
    topLeft = topLeftOffset,
    size = size
  )
}

/**
 * 绘制鼠标的跟随提醒，目前是一个水滴的相机，仿chrome
 */
@Composable
private fun DrawMouseTips(movePoint: Offset) {
  BoxWithConstraints(
    modifier = Modifier.size(30.dp)
      .offset { IntOffset(movePoint.x.toInt(), movePoint.y.toInt()) },
    contentAlignment = Alignment.Center
  ) {
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
    // 内嵌一个 camera 图标
    Box(
      modifier = Modifier.fillMaxSize().padding(8.dp).clip(CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        Icons.Filled.PhotoCamera,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error
      )
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