package org.dweb_browser.browser.desk.render

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/***
 * app跳动动画效果
 */
fun Modifier.jump(
  enable: Boolean,
  height: Float = 8f,
  duration: Int = 600,
  repeatInterval: Int = 700
) = composed {
  val offsetY = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()
  LaunchedEffect(enable) {
    if (enable) {
      scope.launch {
        while (true) {
          // 上升动画
          offsetY.animateTo(
            targetValue = -height,
            animationSpec = tween(
              durationMillis = duration / 2,
              easing = CubicBezierEasing(0.17f, 0.67f, 0.83f, 0.67f) //给自然的加速和减速效果
            )
          )
          // 下降动画
          offsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
              dampingRatio = Spring.DampingRatioMediumBouncy,
              stiffness = Spring.StiffnessLow
            )
          )
          // 等待下一次跳跃
          delay(repeatInterval.toLong() - duration)
        }
      }
    } else {
      // 如果禁用，确保回到初始位置
      offsetY.animateTo(0f, spring())
    }
  }

  val density = LocalDensity.current
  Modifier.graphicsLayer {
    translationY = offsetY.value * density.density
  }
}