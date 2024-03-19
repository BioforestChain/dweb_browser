package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.Render

actual suspend fun ITaskbarView.Companion.create(taskbarController: TaskbarController): ITaskbarView {
  TODO("Not yet implemented")
}

class TaskbarView private constructor(
  private val taskbarController: TaskbarController, override val taskbarDWebView: IDWebView
) : ITaskbarView(taskbarController) {
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