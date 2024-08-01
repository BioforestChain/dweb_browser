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
    Modifier.padding(start = PADDING_VALUE.dp, top = PADDING_VALUE.dp, end = PADDING_VALUE.dp)
      .background(
        Brush.horizontalGradient(
          listOf(
            Color.Transparent, Color.Black, Color.Transparent
          )
        )
      ), color = Color.Transparent
  )
}