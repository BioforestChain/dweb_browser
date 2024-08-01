package org.dweb_browser.browser.desk.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposePanel
import org.dweb_browser.browser.desk.TaskbarV2Controller
import org.dweb_browser.helper.platform.asDesktop

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
    val parentWindow by taskbarController.desktopController.viewController.asDesktop()
      .composeWindowAsState()
    val nativeFloatBar = remember {
      val draggableDelegate = DraggableDelegate()
      NativeMagnetFloatBar(
        state = taskbarController.state,
        runtime = taskbarController.deskNMM,
        content = NativeTaskbarV2Content(draggableDelegate) {
          FloatBarShell(state, draggableDelegate) { modifier ->
            FloatBarMover(draggableDelegate, modifier) {
              RenderContent(draggableDelegate)
            }
          }
        },
        parentWindow = parentWindow,
      )
    }

    SideEffect {
      nativeFloatBar.isVisible = true
    }

    DisposableEffect(Unit) {
      onDispose {
        nativeFloatBar.dispose()
      }
    }

    val layoutWidth by state.layoutWidthFlow.collectAsState()
    val layoutHeight by state.layoutHeightFlow.collectAsState()
    val dragging by state.draggingFlow.collectAsState()
    LaunchedEffect(layoutWidth, layoutHeight) {
      nativeFloatBar.setSize(layoutWidth.toInt(), layoutHeight.toInt())
      if (!nativeFloatBar.dragging) {
        nativeFloatBar.playMagnetEffect()
      }
    }
    nativeFloatBar.dragging = dragging
  }

}