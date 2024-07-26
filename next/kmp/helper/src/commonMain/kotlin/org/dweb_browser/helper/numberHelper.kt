package org.dweb_browser.helper

fun Number.scale(fromRange: Pair<Number, Number>, toRange: Pair<Number, Number>): Double {
  val fromStart = fromRange.first.toDouble()
  val fromEnd = fromRange.second.toDouble()
  val fromCurrent = this.toDouble()
  val current = (fromCurrent - fromStart) / (fromEnd - fromStart)

  val toStart = toRange.first.toDouble()
  val toEnd = toRange.second.toDouble()

  val toCurrent = (toEnd - toStart) * current + toStart
  return toCurrent
}

fun Number.scale(toRange: Pair<Number, Number>) = scale(0 to 1, toRange)

fun Number.scale(fromRange: IntRange, toRange: IntRange) =
  this.scale(fromRange.first to fromRange.last, toRange.first to toRange.last)

fun Number.scale(toRange: IntRange) = scale(0..1, toRange)

fun Number.scale(fromRange: LongRange, toRange: LongRange) =
  this.scale(fromRange.first to fromRange.last, toRange.first to toRange.last)

fun Number.scale(toRange: LongRange) = scale(0L..1L, toRange)


fun Number.scale(
  fromRange: ClosedFloatingPointRange<Double>,
  toRange: ClosedFloatingPointRange<Double>,
) = this.scale(fromRange.start to fromRange.endInclusive, toRange.start to toRange.endInclusive)

fun Number.scale(toRange: ClosedFloatingPointRange<Double>) = scale(0.0..1.0, toRange)


