package org.dweb_browser.helper.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
@Preview
fun FocusableBoxPreview() {
  Row(
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.padding(16.dp)
  ) {
    FocusableBox {
      Box(Modifier.size(100.dp).background(if (isFocused) Color.Red else Color.Blue))
    }
    FocusableBox {
      Box(Modifier.size(100.dp).background(if (isFocused) Color.Red else Color.Blue))
    }
    FocusableBox {
      Box(Modifier.size(100.dp).background(if (isFocused) Color.Red else Color.Blue))
    }
  }
}