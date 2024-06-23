package org.dweb_browser.helper.compose

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.runtime.Stable

val IosEnterEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
val IosLeaveEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
const val IosDefaultDurationMillisIn = 350
const val IosDefaultDurationMillisOut = 500

@Stable
fun <T> iosTween(
  durationIn: Boolean = true,
  durationMillis: Int = if (durationIn) IosDefaultDurationMillisIn else IosDefaultDurationMillisOut,
  delayMillis: Int = 0,
  easing: Easing = if (durationIn) IosEnterEasing else IosLeaveEasing
): TweenSpec<T> = TweenSpec(durationMillis, delayMillis, easing)
