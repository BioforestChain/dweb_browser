package org.dweb_browser.browser.desk.model

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class ActivityStyle(
  val centerWidth: Float,
  val openCenterWidth: Float,
  val overlayCutoutHeight: Float,
  val openOverlayCutoutHeight: Float,
  val screenMarginTop: Float,
  val openScreenMarginTop: Float,
  val radius: Float,
  val openRadius: Float,
  val shadowElevation: Float,
  val openShadowElevation: Float,
) {
  companion object {
    val defaultCutoutOrStatusBarTop: Float
      @Composable get() {
        return WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding().value
      }

    @Composable
    fun common(
      displayWidth: Float,
      radius: Float = 16f,
      openRadius: Float = radius * 2,
      topPadding: Float = 8f,
      shadowElevation: Float = 0f,
      openShadowElevation: Float = 16f,
      cutoutOrStatusBarTop: Float,
      canOverlayCutoutHeight: Float,
    ): ActivityStyle {
      return remember(displayWidth, cutoutOrStatusBarTop, canOverlayCutoutHeight) {
        ActivityStyle(
          centerWidth = 96f,
          openCenterWidth = displayWidth * 0.9f - 48f - 48f - 16f - 16f,
          overlayCutoutHeight = 0f,
          screenMarginTop = cutoutOrStatusBarTop + topPadding,
          openOverlayCutoutHeight = when (canOverlayCutoutHeight) {
            0f -> 0f
            else -> canOverlayCutoutHeight + topPadding
          },
          openScreenMarginTop = when (canOverlayCutoutHeight) {
            0f -> cutoutOrStatusBarTop + topPadding
            else -> cutoutOrStatusBarTop - canOverlayCutoutHeight
          },
          radius = radius,
          openRadius = openRadius,
          shadowElevation = shadowElevation,
          openShadowElevation = openShadowElevation,
        )
      }
    }
  }
}

/// 根据不同平台与不同硬件情况来配置样式
@Composable
expect fun rememberActivityStyle(): ActivityStyle