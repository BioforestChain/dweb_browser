package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.desk.TaskbarV2Controller
import org.dweb_browser.helper.platform.rememberDisplaySize
import org.dweb_browser.sys.window.floatBar.DraggableDelegate
import org.dweb_browser.sys.window.floatBar.FloatBarMover
import org.dweb_browser.sys.window.floatBar.FloatBarShell

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
    /**
     * 桌面端需要在 NativeMagnetFloatBar 外部获得
     */
    val displaySizeState = rememberUpdatedState(rememberDisplaySize())
    CommonTaskbarRender(
      taskbarController, state,
      nativeFloatBarFactory = { parentWindow ->
        val draggableDelegate = DraggableDelegate()
        NativeMagnetFloatBar(
          state = taskbarController.state,
          runtime = taskbarController.deskNMM,
          content = NativeTaskbarV2Content(draggableDelegate) {
            val displaySize by displaySizeState
            FloatBarShell(
              state = state,
              draggableDelegate = draggableDelegate,
              displaySize = displaySize,
              effectBounds = { bounds ->
                this.size(bounds.width.dp, bounds.height.dp)
              }
            ) { modifier ->
              FloatBarMover(draggableDelegate, modifier) {
                RenderContent(draggableDelegate, displaySize = displaySize)
              }
            }
          },
          parentWindow = parentWindow,
        )
      },
    )
  }
}