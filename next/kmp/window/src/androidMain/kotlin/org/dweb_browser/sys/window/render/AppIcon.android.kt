package org.dweb_browser.sys.window.render

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun AppIconContainerPreview() {
  Box(Modifier.padding(20.dp)) {
    AppIconContainer(color = Color.Green, shadow = 5.dp).Render(Modifier.size(64.dp)) {
      Text("G")
    }
  }
}