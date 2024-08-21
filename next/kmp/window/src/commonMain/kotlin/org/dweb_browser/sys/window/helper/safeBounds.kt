package org.dweb_browser.sys.window.helper

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import org.dweb_browser.helper.PureBounds
import org.dweb_browser.sys.window.core.WindowController
import kotlin.math.max

@Composable
internal fun WindowController.safeBounds(limits: WindowLimits): PureBounds {
  val layoutDirection = LocalLayoutDirection.current
  // 这里不要用 watchedBounds，会导致冗余的计算循环
  val bounds = state.bounds

  /**
   * 获取可触摸的空间
   */
  val safeGesturesPadding = WindowInsets.safeGestures.asPaddingValues()
  val winWidth = max(bounds.width, limits.minWidth)
  val safeLeftPadding = safeGesturesPadding.calculateLeftPadding(layoutDirection).value
  val top = safeGesturesPadding.calculateTopPadding().value
  val safeRightPadding = safeGesturesPadding.calculateRightPadding(layoutDirection).value
  val safeBottomPadding = safeGesturesPadding.calculateBottomPadding().value
  val left = safeLeftPadding - winWidth / 2
  val right = limits.maxWidth - safeRightPadding - winWidth / 2
  val bottom = limits.maxHeight - safeBottomPadding - limits.topBarBaseHeight // 确保 topBar 在可触摸的空间内
  return PureBounds(top = top, left = left, bottom = bottom, right = right)
}