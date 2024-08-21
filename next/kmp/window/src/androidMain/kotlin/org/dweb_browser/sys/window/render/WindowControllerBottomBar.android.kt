package org.dweb_browser.sys.window.render

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import org.dweb_browser.sys.window.helper.LocalWindowFrameStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
actual fun windowBottomNavigationThemeBarMaximizedModifier(): Modifier {
  val winFrameStyle = LocalWindowFrameStyle.current
  val density = LocalDensity.current
  val navigationBarsIgnoringVisibility = WindowInsets.navigationBarsIgnoringVisibility
  val navigationBarsHeight = navigationBarsIgnoringVisibility.getBottom(density)
  if (winFrameStyle.frameSize.bottom > navigationBarsHeight / 2) {
    return Modifier
  }
  val navigationBars = WindowInsets.navigationBars
  val p =
    navigationBars.getBottom(density)
      .toFloat() / navigationBarsIgnoringVisibility.getBottom(density)
  return Modifier.alpha(animateFloatAsState(1f - p, label = "alpha").value)
}