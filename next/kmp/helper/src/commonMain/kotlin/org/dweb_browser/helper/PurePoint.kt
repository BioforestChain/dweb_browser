package org.dweb_browser.helper

import kotlinx.serialization.Serializable

@Serializable
class PurePoint(val x: Float, val y: Float) {
  companion object {
    val Zero = PurePoint(0f, 0f)
  }

  fun toMutable() = Mutable(x, y)
  class Mutable(
    var x: Float,
    var y: Float,
  ) {
    fun toImmutable() = PurePoint(x, y)
  }
}