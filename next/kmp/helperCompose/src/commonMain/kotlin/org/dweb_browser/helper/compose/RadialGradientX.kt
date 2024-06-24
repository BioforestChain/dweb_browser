package org.dweb_browser.helper.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex


@Composable
expect fun RadialGradientX(
  modifier: Modifier,
  startX: Float,
  startY: Float,
  startRadius: Float,
  endX: Float,
  endY: Float,
  endRadius: Float,
  colors: Array<Color>,
  stops: Array<Float>? = null,
)

@Composable
fun RadialGradientXDemo() {
  Box(
    modifier = Modifier.background(Color.Yellow).padding(20.dp),
  ) {
    RadialGradientX(
      Modifier.size(400.dp),
      startX = 300f,
      startY = 300f,
      startRadius = 10f,
      endX = 200f,
      endY = 200f,
      endRadius = 200f,
      colors = arrayOf(
        Color.Magenta, Color.Green, Color.Cyan, Color.Transparent
      )
    )
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      RadialGradientX(
        Modifier.size(200.dp),
        startX = 50f,
        startY = 50f,
        startRadius = 1f,
        endX = 100f,
        endY = 100f,
        endRadius = 100f,
        colors = arrayOf(
          Color.Red, Color.Blue
        )
      )
      Spacer(Modifier.size(12.dp))
      RadialGradientX(
        Modifier.size(200.dp),
        startX = 50f,
        startY = 50f,
        startRadius = 1f,
        endX = 100f,
        endY = 100f,
        endRadius = 100f,
        colors = arrayOf(
          Color.Red, Color.Blue, Color.Transparent
        )
      )
    }
    Text(
      "Hello World",
      modifier = Modifier.zIndex(100f).align(Alignment.Center).alpha(0.5f),
      style = TextStyle(fontSize = 60.sp),
    )
  }
}