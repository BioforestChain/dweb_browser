package org.dweb_browser.browser.desk

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.Render
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.getAppContextUnsafe

actual suspend fun ITaskbarView.Companion.create(taskbarController: TaskbarController): ITaskbarView =
  TaskbarView.from(taskbarController)

class TaskbarView private constructor(
  private val taskbarController: TaskbarController, override val taskbarDWebView: IDWebView,
) : ITaskbarView(taskbarController) {
  companion object {
    suspend fun from(taskbarController: TaskbarController): ITaskbarView {
      val dwebView = IDWebView.create(
        context = getAppContextUnsafe(),
        remoteMM = taskbarController.deskNMM,
        options = taskbarController.getTaskbarDWebViewOptions()
      )
      return TaskbarView(taskbarController, dwebView)
    }
  }

  @Composable
  override fun TaskbarViewRender(draggableHelper: DraggableHelper, modifier: Modifier) {
    val density = LocalDensity.current.density
    Box(modifier = modifier.pointerInput(draggableHelper) {
      detectDragGestures(onDragEnd = draggableHelper.onDragEnd,
        onDragCancel = draggableHelper.onDragEnd,
        onDragStart = draggableHelper.onDragStart,
        onDrag = { _, dragAmount ->
          draggableHelper.onDrag(dragAmount.div(density))
        })
    }) {
      taskbarDWebView.Render()
    }
  }

  @Composable
  override fun FloatWindow() {
    val isActivityMode by state.composableHelper.stateOf { floatActivityState }
    if (isActivityMode) {

      /**
       * 全局的浮动窗口，背景高斯模糊
       */
      val context = LocalContext.current
      LaunchedEffect(state.composableHelper) {
        context.startActivity(Intent(
          context, TaskbarActivity::class.java
        ).also { intent ->
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
          intent.putExtras(Bundle().apply {
            putString("deskSessionId", taskbarController.deskSessionId)
          })
        })
      }
    } else {
      NormalFloatWindow()
    }
  }
}
