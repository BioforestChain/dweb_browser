package org.dweb_browser.helper

import kotlinx.serialization.Serializable

@Serializable
public data class PurePoint(val x: Float, val y: Float) {
  public companion object {
    public val Zero: PurePoint = PurePoint(0f, 0f)
  }

  public fun toMutable(): Mutable = Mutable(x, y)
  public class Mutable(
    public var x: Float,
    public var y: Float,
  ) {
    public fun toImmutable(): PurePoint = PurePoint(x, y)
  }

  public fun timesToInt(times: Float): PureIntPoint =
    PureIntPoint((times * x).toInt(), (times * y).toInt())
}

@Serializable
public data class PureIntPoint(
  val x: Int,
  val y: Int,
)