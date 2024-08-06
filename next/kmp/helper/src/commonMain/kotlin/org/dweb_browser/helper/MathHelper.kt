package org.dweb_browser.helper

import kotlin.math.max
import kotlin.math.min

fun clamp(min: Float, value: Float, max: Float) = min(max(min, value), max)
fun clamp(min: Int, value: Int, max: Int) = min(max(min, value), max)
fun clamp(min: Double, value: Double, max: Double) = min(max(min, value), max)
fun clamp(min: Long, value: Long, max: Long) = min(max(min, value), max)
