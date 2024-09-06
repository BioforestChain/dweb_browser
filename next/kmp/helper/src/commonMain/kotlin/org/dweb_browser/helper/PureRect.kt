package org.dweb_browser.helper

import kotlinx.serialization.Serializable

/**
 * 窗口大小与位置
 *
 * 默认值是NaN，这种情况下，窗口构建者需要自己对其进行赋值
 */
@Serializable
public data class PureRect(
  val x: Float = Float.NaN,
  val y: Float = Float.NaN,
  val width: Float = Float.NaN,
  val height: Float = Float.NaN,
) {
  public companion object {
    public val Zero: PureRect = PureRect(0f, 0f, 0f, 0f)
  }

  public fun toMutable(): Mutable = Mutable(x, y, width, height)
  public fun mutable(apply: Mutable.() -> Unit): PureRect = toMutable().run {
    apply()
    toImmutable()
  }

  public class Mutable(
    public var x: Float,
    public var y: Float,
    public var width: Float,
    public var height: Float,
  ) {
    public fun toImmutable(): PureRect = PureRect(x, y, width, height)
  }

  public fun toPureBounds(): PureBounds =
    PureBounds(top = y, left = x, bottom = y + height, right = x + width)

  public fun timesToInt(times: Float): PureIntRect = PureIntRect(
    x = (x * times).toInt(),
    y = (y * times).toInt(),
    width = (width * times).toInt(),
    height = (height * times).toInt(),
  )
}

@Serializable
public data class PureIntRect(
  val x: Int,
  val y: Int,
  val width: Int,
  val height: Int,
) {
  public fun divToFloat(times: Float): PureRect = PureRect(
    x = x / times,
    y = y / times,
    width = width / times,
    height = height / times,
  )
}