package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.desk.TaskbarV2Controller
import org.dweb_browser.helper.platform.asDesktop
import org.dweb_browser.helper.platform.rememberPureViewBox
import org.dweb_browser.sys.window.helper.DraggableDelegate
import org.dweb_browser.sys.window.helper.FloatBarMover
import org.dweb_browser.sys.window.helper.FloatBarShell

actual fun ITaskbarV2View.Companion.create(taskbarController: TaskbarV2Controller): ITaskbarV2View {
  return TaskbarV2View(taskbarController)
}

class TaskbarV2View(taskbarController: TaskbarV2Controller) : ITaskbarV2View(taskbarController) {
  class NativeTaskbarV2Content(
    val draggableDelegate: DraggableDelegate,
    content: @Composable () -> Unit,
  ) :
    NativeFloatBarContent(ComposePanel().apply {
      background = java.awt.Color(0, 0, 0, 0)
      setContent(content)
    }) {
    override fun onEndDrag() {
      draggableDelegate.onDragEnd()
    }
  }

  @Composable
  override fun Render() {
    val viewBox = rememberPureViewBox().asDesktop()
    val displaySize by produceState(Size.Zero) { value = viewBox.getDisplaySize() }
    CommonTaskbarRender(taskbarController, state) { parentWindow ->
      val draggableDelegate = DraggableDelegate()
      NativeMagnetFloatBar(
        state = taskbarController.state,
        runtime = taskbarController.deskNMM,
        content = NativeTaskbarV2Content(draggableDelegate) {
          FloatBarShell(
            state,
            draggableDelegate,
            displaySize = displaySize,
            effectBounds = {bounds ->
              this.size(bounds.width.dp, bounds.height.dp)
            }
          ) { modifier ->
            FloatBarMover(draggableDelegate, modifier) {
              RenderContent(draggableDelegate)
            }
          }
        },
        parentWindow = parentWindow,
      )
    }
  }
}