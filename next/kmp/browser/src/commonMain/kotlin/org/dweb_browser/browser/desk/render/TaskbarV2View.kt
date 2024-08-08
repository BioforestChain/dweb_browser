package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.desk.TaskbarV2Controller
import org.dweb_browser.sys.window.helper.DraggableDelegate

internal const val PADDING_VALUE = 6
internal const val TASKBAR_WIDTH = 54f
internal const val TASKBAR_DIVIDER_HEIGHT = 8f

expect fun ITaskbarV2View.Companion.create(taskbarController: TaskbarV2Controller): ITaskbarV2View

abstract class ITaskbarV2View(protected val taskbarController: TaskbarV2Controller) {
  companion object {}

  val state = taskbarController.state

  @Composable
  protected fun RenderContent(
    draggableDelegate: DraggableDelegate,
    modifier: Modifier = Modifier,
  ) {
    val apps by taskbarController.appsFlow.collectAsState()

    val appCount = apps.size
    remember(appCount) {
      taskbarController.state.layoutWidth = TASKBAR_WIDTH
      taskbarController.state.layoutHeight = when (appCount) {
        0 -> TASKBAR_WIDTH
        else -> appCount * (TASKBAR_WIDTH - PADDING_VALUE) + TASKBAR_DIVIDER_HEIGHT + TASKBAR_WIDTH
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
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {

      apps.forEach { app ->
        @Suppress("DeferredResultUnused")
        TaskBarAppIcon(
          app = app,
          microModule = taskbarController.deskNMM,
          openApp = {
            taskbarController.open(app.mmid)
          },
          quitApp = {
            taskbarController.quit(app.mmid)
          },
          toggleWindow = {
            taskbarController.toggleWindowMaximize(app.mmid)
          },
        )
      }

      if (apps.isNotEmpty()) {
        TaskBarDivider()
      }

      taskBarHomeButton.Render({
        taskbarController.toggleDesktopView()
      }, Modifier.padding(PADDING_VALUE.dp))
    }
  }

  @Composable
  abstract fun Render()
}

