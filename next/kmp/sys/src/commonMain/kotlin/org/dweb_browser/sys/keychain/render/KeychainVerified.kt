package org.dweb_browser.sys.keychain.render

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Verified
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import org.dweb_browser.helper.platform.theme.LocalColorful
import org.dweb_browser.sys.haptics.HapticsNotificationType
import org.dweb_browser.sys.haptics.VibrateManage

@Composable
fun KeychainVerified(modifier: Modifier = Modifier, iconSize: Dp) {
  val vm = remember { VibrateManage() }
  Box(modifier, contentAlignment = Alignment.Center) {
    val animationSpec =
      remember { spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow) }
    val scale = remember { Animatable(0f) }
    val rotate = remember { Animatable(-90f) }
    val alpha = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
      vm.notification(HapticsNotificationType.SUCCESS)
      launch { scale.animateTo(1f, animationSpec) }
      launch { rotate.animateTo(0f, animationSpec) }
      launch { alpha.animateTo(1f, animationSpec) }
    }
    Icon(
      Icons.TwoTone.Verified,
      null,
      tint = LocalColorful.current.Green.current,
      modifier = Modifier.size(iconSize).graphicsLayer {
        this.transformOrigin = TransformOrigin.Center
        this.rotationZ = rotate.value
        this.alpha = alpha.value
        this.scaleX = scale.value
        this.scaleY = scale.value
      }.pointerInput(Unit) {
        val pointerAnimationSpec = spring<Float>(Spring.DampingRatioMediumBouncy)
        detectTapGestures(
          // touchStart
          onPress = {
            vm.vibrateTick()
            scope.launch {
              launch { scale.animateTo(1.1f, pointerAnimationSpec) }
              launch { rotate.animateTo(18f, pointerAnimationSpec) }
            }
          },
          // touchEnd
          onTap = {
            scope.launch {
              launch { scale.animateTo(1f, pointerAnimationSpec) }
              launch { rotate.animateTo(0f, pointerAnimationSpec) }
            }
          },
        )
      }
    )
  }
}
