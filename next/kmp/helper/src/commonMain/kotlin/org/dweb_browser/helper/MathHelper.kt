package org.dweb_browser.helper

import kotlin.math.max
import kotlin.math.min

public fun clamp(min: Float, value: Float, max: Float): Float = min(max(min, value), max)
public fun clamp(min: Int, value: Int, max: Int): Int = min(max(min, value), max)
public fun clamp(min: Double, value: Double, max: Double): Double = min(max(min, value), max)
public fun clamp(min: Long, value: Long, max: Long): Long = min(max(min, value), max)
