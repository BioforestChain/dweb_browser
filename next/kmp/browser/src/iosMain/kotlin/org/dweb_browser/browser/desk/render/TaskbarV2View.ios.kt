package org.dweb_browser.browser.desk.render

import androidx.compose.runtime.Composable
import org.dweb_browser.browser.desk.TaskbarV2Controller

actual fun ITaskbarV2View.Companion.create(taskbarController: TaskbarV2Controller): ITaskbarV2View {
  return TaskbarV2View(taskbarController)
}

class TaskbarV2View(taskbarController: TaskbarV2Controller) : ITaskbarV2View(taskbarController) {
  @Composable
  override fun Render() {
    FloatBarShell(state) { modifier ->
      FloatBarMover(draggableDelegate, modifier) {
        RenderContent(draggableDelegate)
      }
    }
  }
}