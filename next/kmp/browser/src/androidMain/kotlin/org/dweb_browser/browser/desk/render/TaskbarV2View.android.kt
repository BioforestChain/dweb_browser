package org.dweb_browser.browser.desk.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.dweb_browser.browser.desk.AndroidTaskbarView
import org.dweb_browser.browser.desk.TaskbarV2Controller

actual fun ITaskbarV2View.Companion.create(taskbarController: TaskbarV2Controller): ITaskbarV2View {
  return TaskbarV2View(taskbarController)
}

class TaskbarV2View(taskbarController: TaskbarV2Controller) : ITaskbarV2View(taskbarController),
  AndroidTaskbarView {
  @Composable
  override fun Render() {
    FloatBarShell(state) { modifier ->
      FloatBarMover(draggableDelegate, modifier) {
        RenderContent(draggableDelegate)
      }
    }
  }

  @Composable
  override fun InnerRender() {
    RenderContent(remember { DraggableDelegate() })
  }
}
