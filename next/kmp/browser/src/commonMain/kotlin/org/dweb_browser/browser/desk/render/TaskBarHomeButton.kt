package org.dweb_browser.browser.desk.render

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.dweb_browser.helper.compose.hoverCursor

@Composable
internal fun rememberTaskBarHomeButton() = remember { BarHomeButton() }

internal class BarHomeButton {
  private val scaleAni = Animatable(1f)
  var isPressed by mutableStateOf(false)
  private suspend fun press() {
    scaleAni.animateTo(1.1f, deskAniSpec())
  }

  private suspend fun lift() {
    scaleAni.animateTo(1.0f, deskAniSpec())
  }

  @Composable
  fun Render(onClick: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(isPressed) {
      if (isPressed) {
        press()
      } else {
        lift()
      }
    }

    BoxWithConstraints(
      contentAlignment = Alignment.Center,
      modifier = Modifier.scale(scaleAni.value).aspectRatio(1.0f)
    ) {
      val desktopWallpaper = rememberDesktopWallpaper()
      desktopWallpaper.Render(modifier.clip(CircleShape).shadow(3.dp, CircleShape)
        .hoverCursor()
        .pointerInput(Unit) {
          this.detectTapGestures(
            onPress = {
              isPressed = true
            },
            onLongPress = {
              isPressed = false
            },
            onTap = {
              println("QAQ BarHomeButton onTap")
              isPressed = false
              onClick()
              desktopWallpaper.play()
            },
          )
        })
    }
  }
}