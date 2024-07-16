package org.dweb_browser.sys.keychain.render

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CompletableDeferred

@Preview
@Composable
fun RegisterPatternPreview() {
  val viewModel = remember { RegisterPatternViewModel(CompletableDeferred()) }
  RegisterPattern(viewModel, Modifier.fillMaxSize())
}