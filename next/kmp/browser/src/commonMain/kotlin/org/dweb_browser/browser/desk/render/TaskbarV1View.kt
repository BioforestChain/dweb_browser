package org.dweb_browser.browser.desk.render

import androidx.compose.runtime.Composable
import org.dweb_browser.browser.desk.TaskbarV1Controller
import org.dweb_browser.dwebview.IDWebView

expect suspend fun ITaskbarV1View.Companion.create(
  controller: TaskbarV1Controller,
  webview: IDWebView,
): ITaskbarV1View

abstract class ITaskbarV1View(private val taskbarController: TaskbarV1Controller) {
  val state = taskbarController.state

  abstract val taskbarDWebView: IDWebView

  companion object {}

  @Composable
  abstract fun Render()
}
