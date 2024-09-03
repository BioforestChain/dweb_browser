package org.dweb_browser.sys.window.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.helper.LocalWindowControllerTheme
import org.dweb_browser.sys.window.helper.watchedIsMaximized

@Composable
actual fun WindowTopBar(
  win: WindowController, modifier: Modifier,
) {
  val contentColor = LocalWindowControllerTheme.current.topContentColor
  val maximize by win.watchedIsMaximized()
  Box(
    modifier = modifier
      .run {
        when {
          maximize -> this
          else -> windowMoveAble(win)
        }
      }
      .background(
        Brush.verticalGradient(
          colors = listOf(
            contentColor.copy(alpha = 0.2f),
            Color.Transparent,
          )
        )
      )
  ) {
    if (maximize) {
      WindowTopMaximizedBar(win)
    } else {
      WindowTopControllerBar(win)
    }
  }
}