package org.dweb_browser.browser.desk.render

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.desk.desktopWallpaperView

@Composable
internal fun TaskBarHomeIcon(click: () -> Unit) {
  val scaleValue = remember { Animatable(1f) }
  val scope = rememberCoroutineScope()

  fun doClickAnimation() {
    scope.launch {
      scaleValue.animateTo(1.1f)
      scaleValue.animateTo(1.0f)
    }
  }

  BoxWithConstraints(contentAlignment = Alignment.Center, modifier = Modifier.graphicsLayer {
    scaleX = scaleValue.value
    scaleY = scaleValue.value
  }.aspectRatio(1.0f).padding(paddingValue.dp)) {
    desktopWallpaperView(
      4,
      modifier = Modifier.blur(1.dp, BlurredEdgeTreatment.Unbounded).clip(CircleShape)
        .shadow(3.dp, CircleShape)
    ) {
      doClickAnimation()
      click()
    }
  }
}