package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.input.pointer.pointerInput
import org.dweb_browser.browser.desk.TaskbarV2Controller
import org.dweb_browser.helper.compose.toOffset
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.rememberPureViewBox
import org.dweb_browser.helper.toPureRect
import org.dweb_browser.sys.window.helper.DraggableDelegate
import org.dweb_browser.sys.window.helper.FloatBarShell
import org.dweb_browser.sys.window.helper.rememberDisplaySize

actual fun ITaskbarV2View.Companion.create(taskbarController: TaskbarV2Controller): ITaskbarV2View {
  return TaskbarV2View(taskbarController)
}

/**
 * FloatBar 拖拽器，提供手势拖拽的功能
 */
@Composable
private fun TaskbarMover(
  pvc: PureViewController,
  draggableDelegate: DraggableDelegate,
  modifier: Modifier,
  content: @Composable () -> Unit,
) {
  Box(
    modifier.pointerInput(draggableDelegate) {
      var previousPosition = Offset.Zero
      detectDragGestures(
        onDragEnd = draggableDelegate.onDragEnd,
        onDragCancel = draggableDelegate.onDragEnd,
        onDragStart = { pointerPositionPx ->
          val viewPosition = pvc.getPosition().toOffset()
          val pointerPosition = pointerPositionPx / density
          val screenPosition = pointerPosition + viewPosition
          draggableDelegate.onDragStart(screenPosition)
          previousPosition = screenPosition
        },
        onDrag = { change, _ ->
          val viewPosition = pvc.getPosition().toOffset()
          val pointerPosition = change.position / density
          val screenPosition = pointerPosition + viewPosition
          val dragAmount = screenPosition - previousPosition
          previousPosition = screenPosition
          draggableDelegate.onDrag(dragAmount)
        },
      )
    },
    contentAlignment = Alignment.Center,
  ) {
    content()
  }
}

class TaskbarV2View(taskbarController: TaskbarV2Controller) : ITaskbarV2View(taskbarController) {
  private val pvc = PureViewController(fullscreen = false).also { pvc ->
    pvc.addContent {
      val displaySize = rememberDisplaySize()
      FloatBarShell(state, displaySize = displaySize, effectBounds = { bounds ->
        LaunchedEffect(bounds) {
          pvc.setBounds(bounds)
        }
        this
      }) { modifier ->
        TaskbarMover(pvc, draggableDelegate, modifier) {
          /// 渲染内容
          RenderContent(draggableDelegate, displaySize = displaySize)
        }
      }
    }
  }

  @Composable
  override fun Render() {
    /// 切换zIndex
    val viewBox = rememberPureViewBox()
    LaunchedEffect(Unit) {
      nativeViewController.addOrUpdate(pvc, Int.MAX_VALUE - 1000)
      /// 需要给一个初始化的bounds，否则compose默认处于一个0x0的区域，是不会触发渲染的
      pvc.setBounds(viewBox.getDisplaySize().toRect().toPureRect())
    }
  }
}