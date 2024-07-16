package org.dweb_browser.sys.keychain.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CompletableDeferred

@Preview
@Composable
fun RegisterQuestionPreview() {
  val viewModel = remember { RegisterQuestionViewModel(CompletableDeferred()) }
  RegisterQuestion(viewModel)
}

@Preview
@Composable
fun VerifyQuestionPreview() {
  val viewModel = remember { VerifyQuestionViewModel(CompletableDeferred()) }
  VerifyQuestion(viewModel)
}