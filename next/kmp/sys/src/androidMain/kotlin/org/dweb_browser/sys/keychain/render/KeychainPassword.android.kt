package org.dweb_browser.sys.keychain.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CompletableDeferred

@Preview
@Composable
fun RegisterPasswordPreview() {
  val viewModel = remember { RegisterPasswordViewModel(CompletableDeferred()) }
  RegisterPassword(viewModel)
}

@Preview
@Composable
fun VerifyPasswordPreview() {
  val viewModel = remember { VerifyPasswordViewModel(CompletableDeferred()) }
  VerifyPassword(viewModel)
}