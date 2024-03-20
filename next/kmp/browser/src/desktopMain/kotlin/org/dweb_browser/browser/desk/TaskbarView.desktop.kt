package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.Render
import org.dweb_browser.dwebview.create

actual suspend fun ITaskbarView.Companion.create(taskbarController: TaskbarController): ITaskbarView =
  TaskbarView.from(taskbarController)

class TaskbarView private constructor(
  private val taskbarController: TaskbarController, override val taskbarDWebView: IDWebView
) : ITaskbarView(taskbarController) {
  companion object {
    suspend fun from(taskbarController: TaskbarController) =
      TaskbarView(taskbarController, IDWebView.create(taskbarController.deskNMM))

  }

  @Composable
  override fun TaskbarViewRender(draggableHelper: DraggableHelper, modifier: Modifier) {
    // TODO 将拖动反应到窗口位置上
    taskbarDWebView.Render(modifier)
  }

  @Composable
  override fun FloatWindow() {
    NormalFloatWindow()
  }

}