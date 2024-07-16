package org.dweb_browser.sys.keychain.render

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.helper.androidAppContextDeferred

@Preview
@Composable
fun VerifyBiometricsPreview() {
  androidAppContextDeferred.complete(LocalContext.current)
  Column {
    var play by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    if (play) {
      val viewModel = remember { VerifyBiometricsViewModel(CompletableDeferred()) }
      VerifyBiometrics(viewModel)
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