package org.dweb_browser.browser.desk.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.dweb_browser.helper.platform.rememberDisplaySize

@Composable
actual fun rememberActivityStyle(builder: (ActivityStyle.() -> ActivityStyle)?): ActivityStyle {
  val displaySize = rememberDisplaySize()
  return ActivityStyle.common(
    displayWidth = displaySize.width,
    topPadding = 0f,
    cutoutOrStatusBarTop = 24f,
    canOverlayCutoutHeight = 0f,
    builder = remember(builder) {
      {
        copy(
          centerWidth = 48f,
          openCenterWidth = 320f,
        ).let {
          builder?.invoke(it) ?: it
        }
      }
    }
  )
}