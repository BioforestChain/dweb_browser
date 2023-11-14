package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import org.dweb_browser.dwebview.IDWebView

expect suspend fun ITaskbarView.Companion.create(taskbarController: TaskbarController): ITaskbarView

abstract class ITaskbarView(private val taskbarController: TaskbarController) {
  val state = TaskbarState()

  abstract val taskbarDWebView: IDWebView

  companion object {}

  /**
   * 打开悬浮框
   */
  private fun openTaskbarActivity() = if (!state.floatActivityState) {
    state.floatActivityState = true
    true
  } else false

  private fun closeTaskbarActivity() = if (state.floatActivityState) {
    state.floatActivityState = false
    true
  } else false

  suspend fun toggleFloatWindow(openTaskbar: Boolean?): Boolean {
    val toggle = openTaskbar ?: !state.floatActivityState
    // 监听状态是否是float
    taskbarController.getFocusApp()?.let { focusApp ->
      taskbarController.stateSignal.emit(
        TaskbarController.TaskBarState(toggle, focusApp)
      )
    }
    return if (toggle) openTaskbarActivity() else closeTaskbarActivity()
  }

  data class SafeBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
  ) {
    val hCenter get() = left + (right - left) / 2
    val vCenter get() = top + (bottom - top) / 2
  }

  @Composable
  abstract fun FloatWindow()
}

fun Offset.toIntOffset(density: Float) = IntOffset((density * x).toInt(), (density * y).toInt())