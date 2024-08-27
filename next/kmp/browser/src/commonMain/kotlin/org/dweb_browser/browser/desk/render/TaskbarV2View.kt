package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.dweb_browser.browser.desk.TaskbarV2Controller
import org.dweb_browser.browser.desk.model.TaskbarAppModel
import org.dweb_browser.helper.clamp
import org.dweb_browser.sys.window.core.constant.WindowMode
import org.dweb_browser.sys.window.floatBar.DraggableDelegate
import kotlin.math.min

private const val TASKBAR_ICON_MIN_SIZE = 32f
private const val TASKBAR_ICON_MAX_SIZE = 54f
private const val TASKBAR_PADDING_VALUE = 6f
private const val TASKBAR_DIVIDER_HEIGHT = 8f

expect fun ITaskbarV2View.Companion.create(taskbarController: TaskbarV2Controller): ITaskbarV2View

abstract class ITaskbarV2View(protected val taskbarController: TaskbarV2Controller) {
  companion object {}

  val state = taskbarController.state

  @Composable
  protected fun RenderContent(
    draggableDelegate: DraggableDelegate,
    displaySize: Size,
    modifier: Modifier = Modifier,
  ) {
    val scope = rememberCoroutineScope()
    val apps by taskbarController.appsFlow.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }
    val firstItem = apps.firstOrNull() ?: TaskbarAppModel("", icon = null, running = false)

    val appCount = apps.size
    LaunchedEffect(appCount, firstItem) {
      isExpanded = apps.firstOrNull()?.mode != WindowMode.MAXIMIZE
    }

    var taskbarIconSize by remember { mutableStateOf(TASKBAR_ICON_MAX_SIZE) }
    var paddingValue by remember { mutableStateOf(TASKBAR_PADDING_VALUE) }
    var dividerHeight by remember { mutableStateOf(TASKBAR_DIVIDER_HEIGHT) }
    var appIconsHeight by remember { mutableStateOf(0f) }
    LaunchedEffect(appCount, displaySize, isExpanded) {
      taskbarIconSize = clamp(
        TASKBAR_ICON_MIN_SIZE,
        min(displaySize.width, displaySize.height) * 0.14f,
        TASKBAR_ICON_MAX_SIZE
      )
      paddingValue = TASKBAR_PADDING_VALUE * taskbarIconSize / TASKBAR_ICON_MAX_SIZE
      dividerHeight = TASKBAR_DIVIDER_HEIGHT * taskbarIconSize / TASKBAR_ICON_MAX_SIZE

      if (displaySize != Size.Zero) {
        taskbarController.state.layoutWidth = taskbarIconSize
        appIconsHeight = when {
          appCount == 0 -> 0f
          isExpanded -> appCount * (taskbarIconSize - paddingValue)
          else -> taskbarIconSize
        }
        taskbarController.state.layoutHeight = appIconsHeight + when {
          appCount == 0 -> taskbarIconSize
          else -> dividerHeight + taskbarIconSize
        }
      }
    }

    val taskBarHomeButton = rememberTaskBarHomeButton(taskbarController.deskNMM)
    LaunchedEffect(draggableDelegate, taskBarHomeButton) {
      draggableDelegate.dragCallbacks += "free taskbar-home-button" to {
        taskBarHomeButton.isPressed = false
      }
    }

    Column(
      modifier = modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      LazyColumn(
        modifier = modifier.fillMaxWidth().requiredHeight(appIconsHeight.dp).pointerInput(Unit) {
          scope.launch {
            awaitPointerEventScope {
              while (true) {
                val event = awaitPointerEvent()
                if (event.changes.any { it.pressed }) {
                  isExpanded = true
                }
              }
            }
          }
        },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        itemsIndexed(
          apps,
          key = { _, it -> it.mmid },
        ) { index, app ->
          @Suppress("DeferredResultUnused") TaskBarAppIcon(app = app,
            microModule = taskbarController.deskNMM,
            padding = paddingValue.dp,
            openAppOrActivate = {
              println("QAQ mmid=${app.mmid} openAppOrActivate=${app.focus} mode=${app.mode}")

              app.opening = true
              taskbarController.openAppOrActivate(app.mmid).invokeOnCompletion {
                app.opening = false
              }
            },
            quitApp = {
              taskbarController.closeApp(app.mmid)
            },
            toggleWindow = {
              taskbarController.toggleWindowMaximize(app.mmid)
            },
            modifier = Modifier.animateItem().zIndex(apps.size - index - 1f).offset {
              if (!isExpanded) {
                IntOffset(
                  0, ((-taskbarIconSize.toInt() + paddingValue + 2) * index).dp.toPx().toInt()
                )
              } else {
                IntOffset(0, 0)
              }
            })
        }
      }

      if (appCount > 0) {
        TaskBarDivider(paddingValue.dp)
      }

      taskBarHomeButton.Render({
        taskbarController.toggleDesktopView()
        isExpanded = if (firstItem.focus) firstItem.mode != WindowMode.MAXIMIZE else true
      }, Modifier.padding(paddingValue.dp).zIndex(apps.size.toFloat()))
    }
  }

  @Composable
  abstract fun Render()
}

