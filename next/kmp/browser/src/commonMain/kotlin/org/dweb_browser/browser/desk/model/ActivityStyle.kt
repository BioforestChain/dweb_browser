package org.dweb_browser.browser.desk.model

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

data class ActivityStyle(
  val centerWidth: Float = 0f,
  val openCenterWidth: Float = 0f,
  val overlayCutoutHeight: Float = 0f,
  val openOverlayCutoutHeight: Float = 0f,
  val screenMarginTop: Float = 0f,
  val openScreenMarginTop: Float = 0f,
  val radius: Float = 16f,
  val openRadius: Float = radius * 2,
  val shadowElevation: Float = 0f,
  val openShadowElevation: Float = 16f,
  val containerBox: @Composable ContainerScope.(content: @Composable BoxScope.() -> Unit) -> Unit = { content ->
    Box(
      Modifier.offset(y = offsetDp),
      content = content,
    )
  },
  val contentBox: @Composable ContentScope.(content: @Composable BoxScope.() -> Unit) -> Unit = { content ->
    Box(
      modifier,
      contentAlignment = Alignment.Center,
      content = content,
    )
  },
) {
  class ContainerScope(val offsetDp: Dp)
  class ContentScope(val modifier: Modifier)

  companion object {
    val defaultCutoutOrStatusBarTop: Float
      @Composable get() {
        return WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding().value
      }

    @Composable
    fun common(
      displayWidth: Float,
      topPadding: Float = 8f,
      cutoutOrStatusBarTop: Float,
      canOverlayCutoutHeight: Float,
      builder: (ActivityStyle.() -> ActivityStyle)? = null,
    ): ActivityStyle {
      return remember(displayWidth, cutoutOrStatusBarTop, canOverlayCutoutHeight, builder) {
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
        ).let {
          builder?.invoke(it) ?: it
        }
      }
    }
  }
}

/// 根据不同平台与不同硬件情况来配置样式
@Composable
expect fun rememberActivityStyle(builder: (ActivityStyle.() -> ActivityStyle)? = null): ActivityStyle