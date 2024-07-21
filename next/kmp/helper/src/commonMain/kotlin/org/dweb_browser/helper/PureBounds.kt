package org.dweb_browser.helper

import kotlinx.serialization.Serializable

/**
 * 窗口大小与位置
 *
 * 默认值是NaN，这种情况下，窗口构建者需要自己对其进行赋值
 */
@Serializable
data class PureBounds(
  val top: Float = Float.NaN,
  val left: Float = Float.NaN,
  val bottom: Float = Float.NaN,
  val right: Float = Float.NaN,
) {
  companion object {
    val Zero = PureBounds(0f, 0f, 0f, 0f)
  }

  fun toMutable() = Mutable(top, left, bottom, right)
  class Mutable(
    var top: Float,
    var left: Float,
    var bottom: Float,
    var right: Float,
  ) {
    fun toImmutable() = PureBounds(top, left, bottom, right)
  }

  val width get() = right - left
  val height get() = bottom - top
  val centerX get() = (left + right) / 2
  val centerY get() = (top + bottom) / 2

  fun toPureRect() = PureRect(x = left, y = top, width = right - left, height = bottom - top)
  fun centerScale(scale: Float): PureBounds {
    val scaleWidthDiff = width * (scale - 1) / 2
    val scaleHeightDiff = height * (scale - 1) / 2
    return PureBounds(
      left = left - scaleWidthDiff,
      right = right + scaleWidthDiff,
      top = top - scaleHeightDiff,
      bottom = bottom + scaleHeightDiff,
    )
  }

  fun timesToPureIntBounds(value: Float) = PureIntBounds(
    top = (top * value).toInt(),
    left = (left * value).toInt(),
    bottom = (bottom * value).toInt(),
    right = (right * value).toInt(),
  )
}

data class PureIntBounds(
  val top: Int = 0,
  val left: Int = 0,
  val bottom: Int = 0,
  val right: Int = 0,
) {
  fun divToPureBounds(value: Float) = PureBounds(
    top = top / value,
    left = left / value,
    bottom = bottom / value,
    right = right / value,
  )
  val bounds by lazy {  }
}