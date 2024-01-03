package org.dweb_browser.helper.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import org.dweb_browser.helper.compose.LoadingModel.Companion.HandleLenRate
import org.dweb_browser.helper.compose.LoadingModel.Companion.ItemCount
import org.dweb_browser.helper.compose.LoadingModel.Companion.ScaleRate
import org.dweb_browser.helper.compose.LoadingModel.Companion.Vector
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

data class CircleItem(
  val center: Offset,
  val radius: Float
)

class LoadingModel {
  companion object {
    const val ScaleRate = 0.3f // 动画球的缩放大小，0.3 表示正常值得三分之一
    const val ItemDivider = 60f // 每个圆形之间的间隔
    const val Radius = 30f // 圆形半径
    const val ItemCount = 6 // 圆形个数，其中一个是动画，剩余的在界面直接显示
    const val HandleLenRate = 2f
    const val Vector = 0.6f // 控制两个圆连接时候长度，间接控制连接线的粗细，该值为 1 的时候连接线为直线
  }

  val maxLength = (Radius * 2 + ItemDivider) * ItemCount // 计算整个动画的整体宽度
  val circlePaths = mutableStateListOf<CircleItem>()
  private var lastRadius: Float = 0f

  fun initCircles(scaleRate: Float, itemCount: Int, radius: Float, itemDivider: Float) {
    if (lastRadius == radius) return
    lastRadius = radius
    circlePaths.clear()
    circlePaths.add(
      CircleItem(
        center = Offset(radius + itemDivider, radius * (1f + scaleRate)),
        radius = radius * 1.0f / 4 * 3
      )
    )
    for (index in 1 until itemCount) {
      val circleItem = CircleItem(
        center = Offset((radius * 2 + itemDivider) * index, radius * (1f + scaleRate)),
        radius = radius
      )
      circlePaths.add(circleItem)
    }
  }
}

@Composable
fun MetaBallLoadingView(
  modifier: Modifier = Modifier,
  itemCount: Int = ItemCount,
  scaleRate: Float = ScaleRate,
) {
  val loadingModel = remember { LoadingModel() }
  val color = MaterialTheme.colorScheme.primary

  val infiniteTransition = rememberInfiniteTransition()
  val pointX by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    )
  )

  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxWidth()) {
      val sizeWidth = size.width
      val radius = sizeWidth * 1.0f / (itemCount * 2) / 2

      loadingModel.initCircles(scaleRate, itemCount, radius, radius * 2)

      val firstCircle = loadingModel.circlePaths.first().let {
        CircleItem(Offset(sizeWidth * pointX, it.center.y), it.radius)
      }
      val roundRect = RoundRect(
        left = firstCircle.center.x - firstCircle.radius,
        top = firstCircle.center.y - firstCircle.radius,
        right = firstCircle.center.x + firstCircle.radius,
        bottom = firstCircle.center.y + firstCircle.radius,
      )
      drawCircle(color = color, center = roundRect.center, radius = firstCircle.radius)

      for (index in 1 until loadingModel.circlePaths.size) {
        val currentCircle = loadingModel.circlePaths[index]
        MetaBall(currentCircle, firstCircle, ScaleRate, radius * 4f, color)
      }
    }
  }
}

private fun getVector(radians: Float, length: Float): Offset {
  val x = cos(radians) * length
  val y = sin(radians) * length
  return Offset(x, y)
}

/**
 * hypot 是平方和开平方根
 */
private fun getLength(offset: Offset) = hypot(offset.x, offset.y)

/**
 * 计算两个点之间的直线距离
 */
private fun getDistance(start: Offset, end: Offset) = hypot(start.x - end.x, start.y - end.y)

/**
 * @param placeCircle 占位的圆形
 * @param moveCircle 移动的圆形
 * @param vectorLength 控制两个圆连接时候长度，间接控制连接线的粗细，该值为 1 的时候连接线为直线
 * @param maxDistance 两点之间的最大距离，也就是占位圆形和移动圆形中心点之间的最大距离
 */
private fun DrawScope.MetaBall(
  placeCircle: CircleItem,
  moveCircle: CircleItem,
  scaleRate: Float,
  maxDistance: Float,
  color: Color
) {
  val distance = getDistance(placeCircle.center, moveCircle.center) // 获取两圆之间的距离
  val moveRadius = moveCircle.radius
  val placeRadius = if (distance > maxDistance) {
    drawCircle(color = color, center = placeCircle.center, radius = placeCircle.radius)
    placeCircle.radius
  } else {
    val scale = 1 + scaleRate * (1 - distance / maxDistance)
    (placeCircle.radius * scale).apply {
      drawCircle(color = color, center = placeCircle.center, radius = this)
    }
  }
  if (moveRadius == 0f || placeRadius == 0f) return // 这个属于异常情况，直接不执行
  if (distance > maxDistance || distance <= abs(placeRadius - moveRadius)) return // 这个属于两个圆形重合或者操过范围，不需要执行

  // TODO 计算黏合
  val (u1, u2) = if (distance < placeRadius + moveRadius) { // 两个圆形有重叠
    Pair(
      acos((placeRadius * placeRadius + distance * distance - moveRadius * moveRadius) / (2 * placeRadius * distance)),
      acos((moveRadius * moveRadius + distance * distance - placeRadius * placeRadius) / (2 * moveRadius * distance))
    )
  } else Pair(0f, 0f) // 两个圆形分离

  val centerMin = Offset(
    moveCircle.center.x - placeCircle.center.x, moveCircle.center.y - placeCircle.center.y
  )
  val angle1 = atan2(centerMin.y, centerMin.x)
  val angle2 = acos((placeRadius - moveRadius) / distance)
  val angle1a = angle1 + u1 + (angle2 - u1) * Vector
  val angle1b = angle1 - u1 - (angle2 - u1) * Vector
  val angle2a = (angle1 + PI - u2 - (PI - u2 - angle2) * Vector).toFloat()
  val angle2b = (angle1 - PI + u2 + (PI - u2 - angle2) * Vector).toFloat()

  val p1a1 = getVector(angle1a, placeRadius)
  val p1b1 = getVector(angle1b, placeRadius)
  val p2a1 = getVector(angle2a, moveRadius)
  val p2b1 = getVector(angle2b, moveRadius)

  val p1a = Offset(p1a1.x + placeCircle.center.x, p1a1.y + placeCircle.center.y)
  val p1b = Offset(p1b1.x + placeCircle.center.x, p1b1.y + placeCircle.center.y)
  val p2a = Offset(p2a1.x + moveCircle.center.x, p2a1.y + moveCircle.center.y)
  val p2b = Offset(p2b1.x + moveCircle.center.x, p2b1.y + moveCircle.center.y)

  val p1ToP2 = Offset(p1a.x - p2a.x, p1a.y - p2a.y)

  val totalRadius = placeRadius + moveRadius
  var d2: Float = min(Vector * HandleLenRate, getLength(p1ToP2) / totalRadius)
  d2 *= min(1f, distance * 2 / (placeRadius + moveRadius))

  val pi2 = (PI / 2).toFloat()
  val sp1 = getVector(angle1a - pi2, placeRadius * d2)
  val sp2 = getVector(angle2a + pi2, moveRadius * d2)
  val sp3 = getVector(angle2b - pi2, moveRadius * d2)
  val sp4 = getVector(angle1b + pi2, placeRadius * d2)

  val path = Path()
  path.moveTo(p1a.x, p1a.y)
  path.cubicTo(
    x1 = p1a.x + sp1.x, y1 = p1a.y + sp1.y,
    x2 = p2a.x + sp2.x, y2 = p2a.y + sp2.y,
    x3 = p2a.x, y3 = p2a.y
  )
  path.lineTo(p2b.x, p2b.y)
  path.cubicTo(
    x1 = p2b.x + sp3.x, y1 = p2b.y + sp3.y,
    x2 = p1b.x + sp4.x, y2 = p1b.y + sp4.y,
    x3 = p1b.x, y3 = p1b.y
  )
  path.lineTo(p1a.x, p1a.y)
  path.close()
  drawPath(path = path, color = color)
}