package org.dweb_browser.helper.compose.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun Modifier.fadeInOut(
  fadeIn: Boolean,
  initValue: Float = if (fadeIn) 0f else 1f,
  animationSpec: AnimationSpec<Float> = spring(),
  onPlayEnd: (fadeIn: Boolean) -> Unit,
) = this.composed {
  val alpha = remember { Animatable(initValue) }
  LaunchedEffect(fadeIn) {
    val targetScale = if (fadeIn) 1f else 0f
    if (alpha.isRunning || alpha.value != targetScale) {
      alpha.animateTo(targetScale, animationSpec)
      onPlayEnd(!fadeIn)
    }
  }
  this.graphicsLayer {
    this.alpha = alpha.value
  }
}
