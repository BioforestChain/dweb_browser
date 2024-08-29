package org.dweb_browser.browser.desk.model

import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.dweb_browser.helper.platform.rememberDisplaySize

@Composable
actual fun rememberActivityStyle(builder: (ActivityStyle.() -> ActivityStyle)?): ActivityStyle {
  val displaySize = rememberDisplaySize()
  val cutoutOrStatusBarTop = ActivityStyle.defaultCutoutOrStatusBarTop
  val deviceType = "${Build.BRAND}/${Build.MODEL}"
  /// 如果有不适配的机型，就在这里添加特例
  val canOverlayCutoutHeight = when (deviceType) {
    "vivo/V2309A", "vivo/V2308" -> 28f
    else -> cutoutOrStatusBarTop * 0.82f
  }
  val radius = when (deviceType) {
    "google/Pixel 4" -> 20f
    else -> 16f
  }

  /**
   * 如果有刘海，那么说明有黑色区域，那么默认和黑色区域的间隔为 8dp
   */
  val topPadding = when (WindowInsets.displayCutout.asPaddingValues().calculateTopPadding().value) {
    0f -> 0f
    else -> 8f
  }

  return ActivityStyle.common(
    displayWidth = displaySize.width,
    topPadding = topPadding,
    cutoutOrStatusBarTop = cutoutOrStatusBarTop,
    canOverlayCutoutHeight = canOverlayCutoutHeight,
    builder = remember(builder) {
      {
        copy(
          radius = radius,
        ).let {
          builder?.invoke(it) ?: it
        }
      }
    }
  )
}