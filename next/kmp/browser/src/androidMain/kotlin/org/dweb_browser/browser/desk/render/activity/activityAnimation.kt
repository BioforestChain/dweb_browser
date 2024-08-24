package org.dweb_browser.browser.desk.render.activity

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

internal fun <T> activityEnterAnimationSpec() = spring<T>(
  dampingRatio = Spring.DampingRatioLowBouncy,
  stiffness = Spring.StiffnessMedium / 100,
)

internal fun <T> activityExitAnimationSpec() = spring<T>(
  dampingRatio = Spring.DampingRatioNoBouncy,
  stiffness = Spring.StiffnessLow / 100,
)