package org.dweb_browser.sys.window.helper

import org.dweb_browser.helper.compose.compositionChainOf

val LocalWindowLimits = compositionChainOf<WindowLimits>("WindowLimits")

data class WindowLimits(
  val minWidth: Float,
  val minHeight: Float,
  val maxWidth: Float,
  val maxHeight: Float,
  /**
   * 窗口的最小缩放
   *
   * 和宽高不一样，缩放意味着保持宽高不变的情况下，将网页内容缩小，从而可以展示更多的网页内容
   */
  val minScale: Double,
  /**
   * 窗口顶部的基本高度
   */
  val topBarBaseHeight: Float,
  /**
   * 窗口底部的基本高度
   */
  val bottomBarBaseHeight: Float,
)