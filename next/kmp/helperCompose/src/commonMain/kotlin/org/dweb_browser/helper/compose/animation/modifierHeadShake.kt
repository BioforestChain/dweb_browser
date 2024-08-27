package org.dweb_browser.helper.compose.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.keyframes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

val translationXSpec = keyframes {
  durationMillis = 1000
  0f at 0 using EaseInOut
  -6f at 65 using EaseInOut
  5f at 185 using EaseInOut
  -3f at 315 using EaseInOut
  2f at 435 using EaseInOut
  0f at 500 using EaseInOut
}
val rotateYSpec = keyframes {
  durationMillis = 1000
  0f at 0 using EaseInOut
  -9f at 65 using EaseInOut
  7f at 185 using EaseInOut
  -5f at 315 using EaseInOut
  3f at 435 using EaseInOut
  0f at 500 using EaseInOut
}

@Composable
fun Modifier.headShake(play: Boolean, onPlayEnd: () -> Unit) = this.composed {
  val translationX = remember { Animatable(0f) }
  val rotateY = remember { Animatable(0f) }
  LaunchedEffect(play) {
    while (play) {
      translationX.snapTo(7f)
      rotateY.snapTo(10f)
      launch { translationX.animateTo(0f, translationXSpec) }
      rotateY.animateTo(0f, rotateYSpec)
      onPlayEnd()
    }
  }
  this.graphicsLayer {
    this.translationX = translationX.value * density
    this.rotationY = rotateY.value
  }
}