package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun DesktopSearchBarPreview() {
  Box(
    Modifier.background(Brush.linearGradient(listOf(Color.Cyan, Color.Blue))).fillMaxSize(),
    contentAlignment = Alignment.TopCenter
  ) {
    rememberDesktopSearchBar().Render(
      Modifier.padding(vertical = 100.dp)
    )
  }
}