package info.bagen.dwebbrowser.microService.core

import com.google.gson.annotations.JsonAdapter

/**
 * 窗口大小与位置
 *
 * 默认值是NaN，这种情况下，窗口构建者需要自己对其进行赋值
 */
data class WindowBounds(
  val left: Float = Float.NaN,
  val top: Float = Float.NaN,
  val width: Float = Float.NaN,
  val height: Float = Float.NaN,
) {
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