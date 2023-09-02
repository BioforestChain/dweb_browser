package org.dweb_browser.helper.compose

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.runtime.Stable

val IosFastOutSlowInEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
const val IosDefaultDurationMillisIn = 500
const val IosDefaultDurationMillisOut = 300

@Stable
fun <T> iosTween(
  durationIn: Boolean = true,
  durationMillis: Int = if (durationIn) IosDefaultDurationMillisIn else IosDefaultDurationMillisOut,
  delayMillis: Int = 0,
  easing: Easing = IosFastOutSlowInEasing
): TweenSpec<T> = TweenSpec(durationMillis, delayMillis, easing)
