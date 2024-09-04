package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.background
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
internal fun TaskBarDivider(modifier: Modifier) {
  HorizontalDivider(
    modifier.background(
      Brush.horizontalGradient(
        listOf(
          Color.Transparent, Color.Black, Color.Transparent
        )
      )
    ),
    thickness = (1 / LocalDensity.current.density).dp,
    color = Color.Transparent,
  )
}