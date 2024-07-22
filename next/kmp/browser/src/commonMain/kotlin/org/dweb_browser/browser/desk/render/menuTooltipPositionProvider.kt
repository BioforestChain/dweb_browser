package org.dweb_browser.browser.desk.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider

@Composable
fun rememberMenuTooltipPositionProvider(spacingBetweenTooltipAndAnchor: Dp = SpacingBetweenTooltipAndAnchor): MenuTooltipPositionProvider {
  val tooltipAnchorSpacing = with(LocalDensity.current) {
    spacingBetweenTooltipAndAnchor.roundToPx()
  }
  return remember(tooltipAnchorSpacing) {
    MenuTooltipPositionProvider(tooltipAnchorSpacing)
  }
}

internal val SpacingBetweenTooltipAndAnchor = 4.dp

class MenuTooltipPositionProvider(val tooltipAnchorSpacing: Int) : PopupPositionProvider {
  override fun calculatePosition(
    anchorBounds: IntRect,
    windowSize: IntSize,
    layoutDirection: LayoutDirection,
    popupContentSize: IntSize,
  ): IntOffset {
    val x: Int
    if (popupContentSize.width > windowSize.width) {
      x = windowSize.width - popupContentSize.width / 2
    } else if (anchorBounds.center.x < windowSize.width / 2) {
      // 尝试从 锚地 的左边开始
      var left = anchorBounds.left
      // 如果右边溢出了，那么向左平移动，直到放下整个 pop
      if (left + popupContentSize.width > windowSize.width) {
        left = windowSize.width - popupContentSize.width
      }
      x = left
    } else {
      // 尝试从 锚地 的右边开始
      var right = anchorBounds.right
      // 如果左边溢出了，那么向右平移，直到放下整个 pop
      if (right - popupContentSize.width < 0) {
        right = popupContentSize.width
      }
      x = right - popupContentSize.width
    }


    var y = anchorBounds.top - popupContentSize.height - tooltipAnchorSpacing
    if (y < 0)
      y = anchorBounds.bottom + tooltipAnchorSpacing
    return IntOffset(x, y)
  }
}