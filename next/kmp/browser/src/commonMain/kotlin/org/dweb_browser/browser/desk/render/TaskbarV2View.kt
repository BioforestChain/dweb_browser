package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.dweb_browser.browser.desk.TaskbarV2Controller
import org.dweb_browser.helper.clamp
import org.dweb_browser.sys.window.helper.DraggableDelegate
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
    val apps by taskbarController.appsFlow.collectAsState()

    val appCount = apps.size
    var taskbarIconSize by remember { mutableStateOf(TASKBAR_ICON_MAX_SIZE) }
    var paddingValue by remember { mutableStateOf(TASKBAR_PADDING_VALUE) }
    var dividerHeight by remember { mutableStateOf(TASKBAR_DIVIDER_HEIGHT) }
    LaunchedEffect(displaySize) {
      taskbarIconSize = clamp(
        TASKBAR_ICON_MIN_SIZE,
        min(displaySize.width, displaySize.height) * 0.12f,
        TASKBAR_ICON_MAX_SIZE
      )
      paddingValue = TASKBAR_PADDING_VALUE * taskbarIconSize / TASKBAR_ICON_MAX_SIZE
      dividerHeight = TASKBAR_DIVIDER_HEIGHT * taskbarIconSize / TASKBAR_ICON_MAX_SIZE
    }
    remember(appCount, displaySize) {
      if (displaySize != Size.Zero) {
        taskbarController.state.layoutWidth = taskbarIconSize
        taskbarController.state.layoutHeight = when (appCount) {
          0 -> taskbarIconSize
          else -> appCount * (taskbarIconSize - paddingValue) + dividerHeight + taskbarIconSize
        }
      }
    }

    val taskBarHomeButton = rememberTaskBarHomeButton(taskbarController.deskNMM)
    LaunchedEffect(draggableDelegate, taskBarHomeButton) {
      draggableDelegate.dragCallbacks += "free taskbar-home-button" to {
        taskBarHomeButton.isPressed = false
      }
    }

    LazyColumn(
      modifier = modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      itemsIndexed(apps, key = { _, it -> it.mmid }) { index, app ->
        @Suppress("DeferredResultUnused")
        TaskBarAppIcon(
          app = app,
          microModule = taskbarController.deskNMM,
          padding = paddingValue.dp,
          openAppOrActivate = {
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
          modifier = Modifier.animateItem().zIndex(apps.size - index - 1f)
        )
      }

      if (apps.isNotEmpty()) {
        item(key = "divider") {
          TaskBarDivider(paddingValue.dp)
        }
      }

      item(key = "home") {
        taskBarHomeButton.Render({
          taskbarController.toggleDesktopView()
        }, Modifier.padding(paddingValue.dp).zIndex(apps.size.toFloat()))
      }
    }
  }

  @Composable
  abstract fun Render()
}

