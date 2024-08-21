package org.dweb_browser.sys.window.helper

import kotlin.math.max
import kotlin.math.min

/**
 * 计算窗口在对应布局时的内容缩放比例
 * 视图的宽高随着窗口的缩小而缩小，随着窗口的放大而放大，
 * 但这些缩放不是等比的，而是会以一定比例进行换算。
 *
 * 这个行为在桌面端也将会适用
 */
fun calcWindowContentScale(
  limits: WindowLimits, contentWidth: Float, contentHeight: Float,
): Float {
  if (limits.minScale == 1.0) {
    return 1f
  }
  /**
   * 计算进度
   */
  fun calcProgress(from: Float, now: Float, to: Float) =
    min(1f, ((now - from) / (to - from))).toDouble()

  /**
   * 将动画进度还原成所需的缩放值
   */
  fun Double.toScale(minScale: Double, maxScale: Double = 1.0) =
    ((maxScale - minScale) * this) + minScale

  val scaleProgress = max(
    calcProgress(limits.minWidth, contentWidth, limits.maxWidth),
    calcProgress(limits.minHeight, contentHeight, limits.maxHeight),
  )

  return scaleProgress.toScale(limits.minScale).let { if (it.isNaN()) 1f else it.toFloat() }
}