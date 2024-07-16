package org.dweb_browser.sys.keychain.render

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.helper.androidAppContextDeferred

@Preview
@Composable
fun KeychainVerifiedPreview() {
  androidAppContextDeferred.complete(LocalContext.current)
  Column {
    var play by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    if (play) {
      KeychainVerified(Modifier.fillMaxWidth(), 128.dp)
    }
    Button({
      scope.launch {
        play = false
        delay(10)
        play = true
      }
    }) {
      Text("replay")
    }
  }
}