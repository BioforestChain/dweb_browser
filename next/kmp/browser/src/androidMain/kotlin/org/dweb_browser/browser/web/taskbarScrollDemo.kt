package org.dweb_browser.browser.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times

@Preview
@Composable
fun TaskbarScrollDemo() {
  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    val appsScrollState = rememberScrollState()
    Box(
      Modifier
        .width(80.dp)
        .requiredHeightIn(max = 200.dp)
        .verticalScroll(appsScrollState)
        .padding(8.dp)
        .background(Color.Green)
    ) {
      val size = 200
      Box(
        Modifier
          .fillMaxWidth()
          .requiredHeight(size * 20.dp)
          .background(Color.Red)
      ) {
        (1..size).forEachIndexed { index, value ->
          Text(text = "Hi~ $value", Modifier.offset(y = index * 20.dp))
        }
      }
    }
  }
}