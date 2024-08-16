package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import org.dweb_browser.browser.desk.render.TaskbarV1View
import org.dweb_browser.browser.desk.render.TaskbarV2View

interface AndroidTaskbarView {
  @Composable
  fun InnerRender(displaySize: Size)
}

fun TaskbarControllerBase.getAndroidTaskbarView(): AndroidTaskbarView {
  return when (val controller = this) {
    is TaskbarV1Controller -> controller.taskbarView as TaskbarV1View
    is TaskbarV2Controller -> controller.view as TaskbarV2View
  }
}