package org.dweb_browser.browser.desk.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.dweb_browser.browser.desk.TaskbarV1Controller
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.asDesktop
import org.dweb_browser.helper.platform.asDesktop

actual suspend fun ITaskbarV1View.Companion.create(
  controller: TaskbarV1Controller,
  webview: IDWebView,
): ITaskbarV1View = TaskbarV1View(controller, webview)

class TaskbarV1View(
  private val taskbarController: TaskbarV1Controller, override val taskbarDWebView: IDWebView,
) : ITaskbarV1View(taskbarController) {
  class NativeTaskbarV1Content(val webview: IDWebView) :
    NativeFloatBarContent(webview.asDesktop().viewEngine.wrapperView) {
    override fun onEndDrag() {
      /// 强行释放 js 里的状态
      webview.lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
        runCatching {
          webview.evaluateAsyncJavascriptCode("dragEnd()")
        }
      }
    }
  }

  @Composable
  override fun Render() {
    // TODO 将拖动反应到窗口位置上
    val parentWindow by taskbarController.desktopController.viewController.asDesktop()
      .composeWindowAsState()
    val dialog = remember(parentWindow) {
      NativeMagnetFloatBar(
        state = taskbarController.state,
        runtime = taskbarController.deskNMM,
        content = NativeTaskbarV1Content(taskbarDWebView),
        parentWindow = parentWindow,
      )
    }

    SideEffect {
      dialog.isVisible = true
    }

    DisposableEffect(Unit) {
      onDispose {
        dialog.dispose()
      }
    }

    val layoutWidth by state.layoutWidthFlow.collectAsState()
    val layoutHeight by state.layoutHeightFlow.collectAsState()
    val dragging by state.draggingFlow.collectAsState()
    LaunchedEffect(layoutWidth, layoutHeight) {
      dialog.let {
        it.setSize(layoutWidth.toInt(), layoutHeight.toInt())
        if (!it.dragging) {
          it.playMagnetEffect()
        }
      }
    }
    dialog.dragging = dragging
  }
}
