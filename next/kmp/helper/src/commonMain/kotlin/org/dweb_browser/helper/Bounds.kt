package org.dweb_browser.helper

import kotlinx.serialization.Serializable

/**
 * 窗口大小与位置
 *
 * 默认值是NaN，这种情况下，窗口构建者需要自己对其进行赋值
 */
@Serializable
data class Bounds(
  val top: Float = Float.NaN,
  val left: Float = Float.NaN,
  val bottom: Float = Float.NaN,
  val right: Float = Float.NaN,
) {
  companion object {
    val Zero = Bounds(0f, 0f, 0f, 0f)
  }

  fun toMutable() = Mutable(top, left, bottom, right)
  class Mutable(
    var top: Float,
    var left: Float,
    var bottom: Float,
    var right: Float,
  ) {
    fun toImmutable() = Bounds(top, left, bottom, right)
  }
}