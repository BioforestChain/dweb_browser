package org.dweb_browser.helper

import kotlinx.serialization.Serializable

/**
 * 窗口大小与位置
 *
 * 默认值是NaN，这种情况下，窗口构建者需要自己对其进行赋值
 */
@Serializable
data class PureRect(
  val x: Float = Float.NaN,
  val y: Float = Float.NaN,
  val width: Float = Float.NaN,
  val height: Float = Float.NaN,
) {
  companion object {
    val Zero = PureRect(0f, 0f, 0f, 0f)
  }

  fun toMutable() = Mutable(x, y, width, height)
  class Mutable(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
  ) {
    fun toImmutable() = PureRect(x, y, width, height)
  }

  fun toPureBounds() = PureBounds(top = y, left = x, bottom = y + height, right = x + width)

  fun timesToInt(times: Float) = PureIntRect(
    x = (x * times).toInt(),
    y = (y * times).toInt(),
    width = (width * times).toInt(),
    height = (height * times).toInt(),
  )
}

@Serializable
data class PureIntRect(
  val x: Int,
  val y: Int,
  val width: Int,
  val height: Int,
) {
  fun divToFloat(times: Float) = PureRect(
    x = x / times,
    y = y / times,
    width = width / times,
    height = height / times,
  )
}