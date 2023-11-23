package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.nativeRootUIViewController_addOrUpdate
import org.dweb_browser.helper.platform.nativeRootUIViewController_remove
import org.dweb_browser.helper.withMainContext
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIColor
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
          privateNet = true,
          detachedStrategy = DWebViewOptions.DetachedStrategy.Ignore
        ), WKWebViewConfiguration()
      )
      TaskbarView(taskbarController, webView)
    }
  }

  private val pvc = PureViewController().also { pvc ->
    pvc.addContent {
      NormalFloatWindow()
    }
  }
  private val scope = taskbarController.deskNMM.ioAsyncScope

  @Composable
  override fun LocalFloatWindow() {
    DisposableEffect(Unit) {
      scope.launch {
        nativeRootUIViewController_addOrUpdate(pvc, zIndex = Int.MAX_VALUE - 1)
      }
      onDispose {
        scope.launch {
          nativeRootUIViewController_remove(pvc)
        }
      }
    }
  }

  @Composable
  override fun GlobalFloatWindow() {
    LocalFloatWindow()
//    val uiViewController = LocalUIViewController.current
//    LaunchedEffect(state.composableHelper) {
//      uiViewController.addChildViewController(
//        taskbarController.platformContext!!.asIos().uiViewController()
//      )
//    }
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