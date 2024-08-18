package org.dweb_browser.helper.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import org.dweb_browser.helper.PureBounds

@Composable
fun WindowInsets.asPureBounds(density: Density = LocalDensity.current): PureBounds {
  val direction = LocalLayoutDirection.current
  return remember(density, direction, this) {
    PureBounds(
      top = getTop(density) / density.density,
      left = getLeft(density, direction) / density.density,
      bottom = getBottom(density) / density.density,
      right = getRight(density, direction) / density.density,
    )
  }
}