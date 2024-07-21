package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.dweb_browser.browser.desk.ITaskbarView
import org.dweb_browser.browser.desk.TaskbarController
import org.dweb_browser.browser.desk.model.TaskbarAppModel
import org.dweb_browser.helper.collectIn
import org.dweb_browser.sys.window.core.helper.pickLargest
import org.dweb_browser.sys.window.core.helper.toStrict

internal const val paddingValue = 6
internal const val taskBarWidth = 55f
internal const val taskBarDividerHeight = 8f

@Composable
fun NewTaskbarView(
  taskbarController: TaskbarController,
  draggableHelper: ITaskbarView.DraggableHelper,
  modifier: Modifier,
) {

  val apps = remember { mutableStateListOf<TaskbarAppModel>() }
  val scope = rememberCoroutineScope()

  fun updateTaskBarSize(appCount: Int) {
    taskbarController.state.layoutWidth = taskBarWidth
    taskbarController.state.layoutHeight = when (appCount) {
      0 -> taskBarWidth
      else -> appCount * (taskBarWidth - paddingValue) + taskBarDividerHeight + taskBarWidth
    }
  }

  fun doGetApps() {
    scope.launch {
      val taskbarApps = taskbarController.getTaskbarAppList(Int.MAX_VALUE).map { new ->
        val oldApp = apps.firstOrNull { old ->
          old.mmid == new.mmid
        }
        TaskbarAppModel(
          new.mmid,
          new.icons.toStrict().pickLargest(),
          new.running,
          oldApp?.isShowClose ?: false,
        )
      }
      apps.clear()
      apps.addAll(taskbarApps)
      updateTaskBarSize(taskbarApps.count())
    }
  }

  DisposableEffect(Unit) {
    val job = taskbarController.onUpdate.run {
      filter { it != "bounds" }
    }.collectIn(scope) {
      doGetApps()
    }

    onDispose {
      job.cancel()
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
        TaskBarAppIcon(Modifier,
          app,
          taskbarController.deskNMM,
          openApp = { mmid ->
            scope.launch {
              taskbarController.open(mmid)
            }
          }, quitApp = { mmid ->
            scope.launch {
              taskbarController.quit(mmid)
            }
          }, toggleWindow = { mmid ->
            scope.launch {
              taskbarController.toggleWindowMaximize(mmid)
            }
          })
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
