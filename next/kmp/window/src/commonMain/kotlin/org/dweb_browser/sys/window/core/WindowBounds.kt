package org.dweb_browser.sys.window.core

import kotlinx.serialization.Serializable

/**
 * 窗口大小与位置
 *
 * 默认值是NaN，这种情况下，窗口构建者需要自己对其进行赋值
 */
@Serializable
data class WindowBounds(
  val left: Float = Float.NaN,
  val top: Float = Float.NaN,
  val width: Float = Float.NaN,
  val height: Float = Float.NaN,
) {
  companion object {
    val Zero = WindowBounds(0f, 0f, 0f, 0f)
  }

  fun toMutable() = Mutable(left, top, width, height)
  class Mutable(
    var left: Float,
    var top: Float,
    var width: Float,
    var height: Float,
  ) {
    fun toImmutable() = WindowBounds(left, top, width, height)
  }
}