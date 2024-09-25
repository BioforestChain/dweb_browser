package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import org.dweb_browser.browser.desk.TaskbarV2Controller
import org.dweb_browser.browser.desk.model.TaskbarAppModel
import org.dweb_browser.helper.platform.rememberDisplaySize
import org.dweb_browser.sys.window.core.constant.WindowMode
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
  ) : NativeFloatBarContent(ComposePanel().apply {
    background = java.awt.Color(0, 0, 0, 0)
    setContent(content)
  }) {
    override fun onEndDrag() {
      draggableDelegate.onDragEnd()
    }
  }

  @OptIn(FlowPreview::class)
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
          state = state,
          runtime = taskbarController.deskNMM,
          content = NativeTaskbarV2Content(draggableDelegate) {
            val displaySize by displaySizeState
            val apps by taskbarController.appsFlow.collectAsState()
            val dragging = state.dragging
            val firstItem = apps.firstOrNull() ?: TaskbarAppModel("", icon = null, running = false)

            /**
             * 1. 当taskbar处于拖拽状态时，处于正常Normal状态
             * 2. 当处于全屏模式时，处于Immersive状态，否则Normal状态
             */
            val taskbarShape = when (dragging) {
              true -> TaskbarShape.NORMAL
              else -> when (firstItem.state.mode) {
                WindowMode.MAXIMIZE, WindowMode.FULLSCREEN -> TaskbarShape.IMMERSIVE
                else -> TaskbarShape.NORMAL
              }
            }

            var isHidden by remember { mutableStateOf(false) }

            LaunchedEffect(taskbarShape, dragging) {
              isHidden = when (taskbarShape) {
                TaskbarShape.NORMAL -> false
                TaskbarShape.IMMERSIVE -> true
              }
            }

            var dragEnd by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
              draggableDelegate.dragStartCallbacks += "immersive" to {
                dragEnd = false
              }
              draggableDelegate.dragEndCallbacks += "immersive" to {
                dragEnd = true
              }
            }

            val taskbarWidth by state.layoutWidthFlow.collectAsState()
            /// 在贴边之后切换为沉浸式模式的样式
            var taskbarAnimatedFinished by remember { mutableStateOf(true) }
            LaunchedEffect(draggableDelegate) {
              state.offsetXFlow.debounce(300).collect { _ ->
                taskbarAnimatedFinished = !state.dragging
              }
            }

            val modifier = when (taskbarShape) {
              TaskbarShape.IMMERSIVE -> Modifier.pointerInput(draggableDelegate) {
                awaitPointerEventScope {
                  while (true) {
                    val event = awaitPointerEvent()
                    /// 这个dragEnd判断必须在awaitPointerEvent之后，否则会导致FloatBarMover无法拖拽
                    if (!dragEnd) continue
                    val position = event.changes.first().position
                    isHidden = !(position.x > 0 && position.x < taskbarWidth)
                  }
                }
              }

              TaskbarShape.NORMAL -> Modifier
            }

            Box(modifier) {
              if (taskbarAnimatedFinished && isHidden) {
                BoxWithConstraints(
                  Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart
                ) {
                  Icon(
                    when (state.offsetX > displaySize.width / 2) {
                      true -> Icons.AutoMirrored.Outlined.ArrowBackIos
                      false -> Icons.AutoMirrored.Outlined.ArrowForwardIos
                    },
                    "Taskbar Immersive",
                    modifier = Modifier.requiredSize(maxWidth * 2 / 3, maxHeight * 2 / 3),
                    tint = MaterialTheme.colors.primarySurface.copy(0.5f)
                  )
                }
              } else {
                FloatBarShell(state = state,
                  draggableDelegate = draggableDelegate,
                  displaySize = displaySize,
                  effectBounds = { bounds ->
                    this.size(bounds.width.dp, bounds.height.dp)
                  }) { modifier ->
                  FloatBarMover(draggableDelegate, modifier) {
                    RenderContent(
                      draggableDelegate,
                      displaySize = safeBounds.size,
                      scrollMaskColor = backgroundColor,
                    )
                  }
                }
              }
            }
          },
          parentWindow = parentWindow,
        )
      },
    )
  }
}