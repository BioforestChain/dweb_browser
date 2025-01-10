package org.dweb_browser.helper.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun SwipeToViewBoxPreview() {
  Column(Modifier.focusGroup()) {
    SwipeToViewBox(backgroundContent = { p ->
      Box(
        Modifier
          .fillMaxSize()
          .background(Color.Cyan), contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.Check, null, Modifier.graphicsLayer {
          scaleX = p
          scaleY = p
          alpha = p
        })
      }
    }) {
      Box(
        Modifier
          .size(300.dp, 100.dp)
          .background(Color.White.copy(alpha = .8f)),
        contentAlignment = Alignment.Center
      ) {
        Text("Hello world")
      }
    }

    HorizontalDivider()

    SwipeToViewBox(backgroundContent = { p ->
      Box(
        Modifier
          .fillMaxHeight()
          .width(100.dp)
          .background(Color.Cyan),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.Check, null, Modifier.graphicsLayer {
          scaleX = p
          scaleY = p
          alpha = p
        })
      }
    }) {
      Box(
        Modifier
          .size(300.dp, 100.dp)
          .background(Color.White.copy(alpha = .8f)),
        contentAlignment = Alignment.Center
      ) {
        Text("Hello world")
      }
    }

    HorizontalDivider()

    val state = rememberSwipeToViewBoxState()

    @Composable
    fun buttons() {
      Row {
        TextButton({ state.openJob() }) { Text(("Open")) }
        TextButton({ state.closeJob() }) { Text(("Close")) }
      }
    }
    SwipeToViewBox(state, backgroundContent = { p ->
      Box(
        Modifier
          .fillMaxHeight()
          .background(Color.Cyan)
      ) {
        buttons()
      }
    }) {
      Box(
        Modifier
          .size(300.dp, 100.dp)
          .background(Color.White.copy(alpha = .8f)),
        contentAlignment = Alignment.Center
      ) {
        buttons()
      }
    }
  }
}
