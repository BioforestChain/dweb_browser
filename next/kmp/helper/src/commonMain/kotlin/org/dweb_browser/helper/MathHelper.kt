package org.dweb_browser.helper

import kotlin.math.max
import kotlin.math.min

fun clamp(min: Float, value: Float, max: Float) = min(max(min, value), max)