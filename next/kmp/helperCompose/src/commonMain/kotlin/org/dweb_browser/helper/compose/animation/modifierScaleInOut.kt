package org.dweb_browser.helper.compose.animation


import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer


@Composable
fun Modifier.scaleInOut(
  scaleIn: Boolean,
  initValue: Float = if (scaleIn) 0f else 1f,
  animationSpec: AnimationSpec<Float> = spring(Spring.DampingRatioLowBouncy),
  onPlayEnd: (scaleIn: Boolean) -> Unit,
) = this.composed {
  val scale = remember { Animatable(initValue) }
  LaunchedEffect(scaleIn) {
    val targetScale = if (scaleIn) 1f else 0f
    if (scale.isRunning || scale.value != targetScale) {
      scale.animateTo(targetScale, animationSpec)
      onPlayEnd(!scaleIn)
    }
  }
  this.graphicsLayer {
    this.scaleX = scale.value
    this.scaleY = scale.value
  }
}
