package org.dweb_browser.browser.desk.render

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun BezGradient(color: Color, modifier: Modifier, style: DrawStyle = Fill, random: Float = 20f) {
  fun toCanvasCoordinate(point: Offset, center: Offset): Offset {
    return Offset(point.x + center.x, point.y + center.y)
  }

  fun randomPolarPoint(degree: Float, oR: Float, iR: Float): Offset {
    val oY = oR * sin(degree / 180f * PI)
    val iY = iR * sin(degree / 180f * PI)

    val oX = oR * cos(degree / 180f * PI)
    val iX = iR * cos(degree / 180f * PI)

    val randomX = Random.nextFloat()
    val randomY = Random.nextFloat()


    val x = when (degree) {
      in 0f..90f -> randomX * (oX - iX) + iX
      in 270f..360f -> randomX * (oX - iX) + iX
      else -> randomX * (iX - oX) + oX
    }.toInt()

    val y = when (degree) {
      in 0f..180f -> randomY * (oY - iY) + iY
      else -> randomY * (oY - iY) + iY
    }.toInt()

    return Offset(x.toFloat(), y.toFloat())
  }

  fun allDegress(number: Int): List<Float> {
    val result = mutableListOf<Float>()
    val step = 360f / number.toFloat()
    var i = 0
    while (i < number) {
      result.add(step * i)
      i++;
    }
    return result
  }

  fun bezierEndPoint(c0: Offset, c1: Offset): Offset {
    return Offset((c1.x - c0.x) / 2 + c0.x, (c1.y - c0.y) / 2 + c0.y)
  }

  fun getPath(center: Offset, radius: Float): Path {
    val points = allDegress(8).map {
      randomPolarPoint(it, radius.toFloat(), radius.toFloat() - random)
    }.map {
      toCanvasCoordinate(it, center)
    }

    val path = Path()

    var start = points[0]
    var c0 = Offset.Zero
    var c1 = Offset.Zero
    var end = Offset.Zero

    var index = 0
    var count = points.count()

    path.moveTo(start.x, start.y)

    while ((index + 1) < count) {
      c0 = points[index]
      c1 = points[index + 1]
      end = bezierEndPoint(c0, c1)
      path.quadraticTo(c0.x, c0.y, end.x, end.y)
      index++
    }
    val last = points.last()
    path.quadraticTo(last.x, last.y, start.x, start.y)
    path.close()

    return path
  }

  var path by remember {
    mutableStateOf<Path?>(null)
  }

  AnimatedContent(path) {
    Canvas(
      modifier.fillMaxSize().clip(CircleShape)
    ) {

      val center = Offset(size.width / 2.0f, size.height / 2.0f)
      val radius = size.minDimension / 2.0
      val path0 = getPath(center, radius.toFloat())
      drawPath(path0, color, style = style)
    }
  }
}