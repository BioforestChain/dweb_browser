package org.dweb_browser.helper

import kotlinx.serialization.Serializable

/**
 * 窗口大小与位置
 *
 * 默认值是NaN，这种情况下，窗口构建者需要自己对其进行赋值
 */
@Serializable
public data class PureBounds(
  val top: Float = Float.NaN,
  val left: Float = Float.NaN,
  val bottom: Float = Float.NaN,
  val right: Float = Float.NaN,
) {
  public companion object {
    public val Zero: PureBounds = PureBounds(0f, 0f, 0f, 0f)
  }

  public fun toMutable(): Mutable = Mutable(top, left, bottom, right)
  public class Mutable(
    public var top: Float,
    public var left: Float,
    public var bottom: Float,
    public var right: Float,
  ) {
    public fun toImmutable(): PureBounds = PureBounds(top, left, bottom, right)
  }

  val width: Float get() = right - left
  val height: Float get() = bottom - top
  val centerX: Float get() = (left + right) / 2
  val centerY: Float get() = (top + bottom) / 2

  public fun toPureRect(): PureRect =
    PureRect(x = left, y = top, width = right - left, height = bottom - top)

  public fun centerScale(scale: Float): PureBounds {
    val scaleWidthDiff = width * (scale - 1) / 2
    val scaleHeightDiff = height * (scale - 1) / 2
    return PureBounds(
      left = left - scaleWidthDiff,
      right = right + scaleWidthDiff,
      top = top - scaleHeightDiff,
      bottom = bottom + scaleHeightDiff,
    )
  }

  public fun timesToPureIntBounds(value: Float): PureIntBounds = PureIntBounds(
    top = (top * value).toInt(),
    left = (left * value).toInt(),
    bottom = (bottom * value).toInt(),
    right = (right * value).toInt(),
  )
}

public data class PureIntBounds(
  val top: Int = 0,
  val left: Int = 0,
  val bottom: Int = 0,
  val right: Int = 0,
) {
  public fun divToPureBounds(value: Float): PureBounds = PureBounds(
    top = top / value,
    left = left / value,
    bottom = bottom / value,
    right = right / value,
  )
}