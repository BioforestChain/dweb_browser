package org.dweb_browser.browser.desk.render.activity

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

internal var animationStiffness by mutableFloatStateOf(1f)
internal fun <T> activityEnterAnimationSpec() = spring<T>(
  dampingRatio = Spring.DampingRatioLowBouncy,
  stiffness = Spring.StiffnessMedium / animationStiffness,
)

internal fun <T> activityExitAnimationSpec() = spring<T>(
  dampingRatio = Spring.DampingRatioNoBouncy,
  stiffness = Spring.StiffnessLow / animationStiffness,
)
