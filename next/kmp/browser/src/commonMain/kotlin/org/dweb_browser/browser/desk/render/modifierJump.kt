package org.dweb_browser.browser.desk.render

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCirc
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Modifier.jump(
  enable: Boolean,
  height: Float = 12f,
  internalDelay: Long = 75
) = this.composed {
  val offsetAni = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()
  val aniState = rememberUpdatedState(enable)
  val heightState = rememberUpdatedState(height)
  val internalDelayState = rememberUpdatedState(internalDelay)

  var animating by remember { mutableStateOf(false) }
  if (enable && !animating) {
    remember {
      scope.launch {
        animating = true
        val backSpec = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessVeryLow)
        while (aniState.value) {
          offsetAni.animateTo(
            heightState.value,
            tween((heightState.value * 50).toInt(), easing = EaseOutCirc)
          )
          offsetAni.animateTo(0f, backSpec)
          delay(internalDelayState.value)
        }
        animating = false
      }
    }
  }
  val density = LocalDensity.current
  graphicsLayer { translationY = -offsetAni.value * density.density }
}