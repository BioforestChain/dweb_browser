package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.desk.ITaskbarView
import org.dweb_browser.browser.desk.TaskbarController

internal const val paddingValue = 6
internal const val taskBarWidth = 55f
internal const val taskBarDividerHeight = 8f

@Composable
fun NewTaskbarView(
  taskbarController: TaskbarController,
  draggableHelper: ITaskbarView.DraggableHelper,
  modifier: Modifier,
) {
  val apps by taskbarController.appsFlow.collectAsState()
  val scope = rememberCoroutineScope()

  val appCount = apps.size
  remember(appCount) {
    taskbarController.state.layoutWidth = taskBarWidth
    taskbarController.state.layoutHeight = when (appCount) {
      0 -> taskBarWidth
      else -> appCount * (taskBarWidth - paddingValue) + taskBarDividerHeight + taskBarWidth
    }
  }

  val taskBarHomeButton = rememberTaskBarHomeButton();
  Box(
    modifier.pointerInput(draggableHelper) {
      detectDragGestures(onDragEnd = draggableHelper.onDragEnd,
        onDragCancel = draggableHelper.onDragEnd,
        onDragStart = { draggableHelper.onDragStart(it) },
        onDrag = { _, dragAmount ->
          draggableHelper.onDrag(dragAmount.div(density))
          taskBarHomeButton.isPressed = false
        })
    }, contentAlignment = Alignment.Center
  ) {

    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {

      apps.forEach { app ->
        TaskBarAppIcon(
          modifier = Modifier,
          app = app,
          microModule = taskbarController.deskNMM,
          openApp = {
            scope.launch { taskbarController.open(app.mmid) }
          },
          quitApp = {
            scope.launch { taskbarController.quit(app.mmid) }
          },
          toggleWindow = {
            scope.launch { taskbarController.toggleWindowMaximize(app.mmid) }
          },
        )
      }

      if (apps.isNotEmpty()) {
        TaskBarDivider()
      }

      taskBarHomeButton.Render({
        scope.launch {
          taskbarController.toggleDesktopView()
        }
      }, Modifier.padding(paddingValue.dp))
    }
  }
}
