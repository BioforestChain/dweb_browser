package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun TaskBarDivider() {
  HorizontalDivider(
    Modifier.padding(start = paddingValue.dp, top = paddingValue.dp, end = paddingValue.dp)
      .background(
        Brush.horizontalGradient(
          listOf(
            Color.Transparent, Color.Black, Color.Transparent
          )
        )
      ), color = Color.Transparent
  )
}