package org.dweb_browser.browser.desk.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import org.dweb_browser.browser.desk.AndroidTaskbarView
import org.dweb_browser.browser.desk.TaskbarV2Controller
import org.dweb_browser.sys.window.floatBar.DraggableDelegate
import org.dweb_browser.sys.window.floatBar.FloatBarMover
import org.dweb_browser.sys.window.floatBar.FloatBarShell

actual fun ITaskbarV2View.Companion.create(taskbarController: TaskbarV2Controller): ITaskbarV2View {
  return TaskbarV2View(taskbarController)
}

class TaskbarV2View(taskbarController: TaskbarV2Controller) : ITaskbarV2View(taskbarController),
  AndroidTaskbarView {
  @Composable
  override fun Render() {
    val displaySize = rememberAndroidDisplaySize()
    FloatBarShell(
      state, displaySize = displaySize
    ) { modifier ->
      FloatBarMover(draggableDelegate, modifier) {
        RenderContent(
          draggableDelegate,
          displaySize = safeBounds.size,
          scrollMaskColor = backgroundColor,
        )
      }
    }
  }

  @Composable
  override fun InnerRender(displaySize: Size) {
    RenderContent(remember { DraggableDelegate() }, displaySize = displaySize)
  }
}
