package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.interop.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.platform.asIos
import org.dweb_browser.helper.withMainContext
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIColor
import platform.UIKit.addChildViewController
import platform.WebKit.WKWebViewConfiguration

actual suspend fun ITaskbarView.Companion.create(taskbarController: TaskbarController): ITaskbarView =
  TaskbarView.from(taskbarController)

class TaskbarView private constructor(
  private val taskbarController: TaskbarController, override val taskbarDWebView: IDWebView
) : ITaskbarView(taskbarController) {
  companion object {
    @OptIn(ExperimentalForeignApi::class)
    suspend fun from(taskbarController: TaskbarController) = withMainContext {
      val webView = IDWebView.create(
        CGRectMake(0.0, 0.0, 100.0, 100.0), taskbarController.deskNMM, DWebViewOptions(
          url = taskbarController.getTaskbarUrl().toString(),
//        onDetachedFromWindowStrategy = DWebViewOptions.DetachedFromWindowStrategy.Ignore,
        ), WKWebViewConfiguration()
      )
      TaskbarView(taskbarController, webView)
    }
  }

  @Composable
  override fun LocalFloatWindow() {
    NormalFloatWindow()
  }

  @Composable
  override fun GlobalFloatWindow() {
    val uiViewController = LocalUIViewController.current
    LaunchedEffect(state.composableHelper) {
      uiViewController.addChildViewController(
        taskbarController.platformContext!!.asIos().uiViewController()
      )
    }
  }
}

fun UIColor.Companion.fromColorInt(color: Int): UIColor {
  return UIColor(
    red = ((color ushr 16) and 0xFF) / 255.0,
    green = ((color ushr 8) and 0xFF) / 255.0,
    blue = ((color) and 0xFF) / 255.0,
    alpha = 1.0
  )
}