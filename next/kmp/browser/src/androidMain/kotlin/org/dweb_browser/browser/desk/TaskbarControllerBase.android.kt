package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import org.dweb_browser.browser.desk.render.TaskbarV1View
import org.dweb_browser.browser.desk.render.TaskbarV2View

interface AndroidTaskbarView {
  @Composable
  fun InnerRender()
}

fun TaskbarControllerBase.getAndroidTaskbarView(): AndroidTaskbarView {
  return when (val controller = this) {
    is TaskbarV1Controller -> controller.taskbarView as TaskbarV1View
    is TaskbarV2Controller -> controller.view as TaskbarV2View
  }
}