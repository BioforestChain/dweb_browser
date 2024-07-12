package org.dweb_browser.helper.compose.animation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun HeadShakePreview() {
  var play by remember { mutableStateOf(false) }
  Button({ play = true }, Modifier.padding(30.dp).headShake(play) { play = false }) {
    Text("click me")
  }
}