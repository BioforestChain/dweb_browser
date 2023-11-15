package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import org.dweb_browser.dwebview.IDWebView

expect suspend fun ITaskbarView.Companion.create(taskbarController: TaskbarController): ITaskbarView

abstract class ITaskbarView(private val taskbarController: TaskbarController) {
  val state = taskbarController.state

  abstract val taskbarDWebView: IDWebView

  companion object {}


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