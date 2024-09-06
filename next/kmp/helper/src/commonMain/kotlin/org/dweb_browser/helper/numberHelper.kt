package org.dweb_browser.helper

public fun Number.scale(fromRange: Pair<Number, Number>, toRange: Pair<Number, Number>): Double {
  val fromStart = fromRange.first.toDouble()
  val fromEnd = fromRange.second.toDouble()
  val fromCurrent = this.toDouble()
  val current = (fromCurrent - fromStart) / (fromEnd - fromStart)

  val toStart = toRange.first.toDouble()
  val toEnd = toRange.second.toDouble()

  val toCurrent = (toEnd - toStart) * current + toStart
  return toCurrent
}

public fun Number.scale(toRange: Pair<Number, Number>): Double = scale(0 to 1, toRange)

public fun Number.scale(fromRange: IntRange, toRange: IntRange): Double =
  this.scale(fromRange.first to fromRange.last, toRange.first to toRange.last)

public fun Number.scale(toRange: IntRange): Double = scale(0..1, toRange)

public fun Number.scale(fromRange: LongRange, toRange: LongRange): Double =
  this.scale(fromRange.first to fromRange.last, toRange.first to toRange.last)

public fun Number.scale(toRange: LongRange): Double = scale(0L..1L, toRange)


public fun Number.scale(
  fromRange: ClosedFloatingPointRange<Double>,
  toRange: ClosedFloatingPointRange<Double>,
): Double =
  this.scale(fromRange.start to fromRange.endInclusive, toRange.start to toRange.endInclusive)

public fun Number.scale(toRange: ClosedFloatingPointRange<Double>): Double =
  scale(0.0..1.0, toRange)
