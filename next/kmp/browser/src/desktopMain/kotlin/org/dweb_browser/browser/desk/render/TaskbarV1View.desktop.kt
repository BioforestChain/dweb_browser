package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.ComposeWindow
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.dweb_browser.browser.desk.TaskbarControllerBase
import org.dweb_browser.browser.desk.TaskbarV1Controller
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.Render
import org.dweb_browser.helper.platform.asDesktop
import org.dweb_browser.sys.window.floatBar.FloatBarState

actual suspend fun ITaskbarV1View.Companion.create(
  controller: TaskbarV1Controller,
  webview: IDWebView,
): ITaskbarV1View = TaskbarV1View(controller, webview)

class TaskbarV1View(
  internal val taskbarController: TaskbarV1Controller, override val taskbarDWebView: IDWebView,
) : ITaskbarV1View(taskbarController) {
  class NativeTaskbarV1Content(val webview: IDWebView) :
    NativeFloatBarContent(ComposePanel().apply {
      background = java.awt.Color(0, 0, 0, 0)
      setContent {
        webview.Render(Modifier.fillMaxSize())
      }
    }) {
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
    CommonTaskbarRender(taskbarController, state) { parentWindow ->
      NativeMagnetFloatBar(
        state = taskbarController.state,
        runtime = taskbarController.deskNMM,
        content = NativeTaskbarV1Content(taskbarDWebView),
        parentWindow = parentWindow,
      )
    }
  }
}

/**
 * 桌面端的通用 taskbar 渲染器
 */
@Composable
internal fun CommonTaskbarRender(
  taskbarController: TaskbarControllerBase,
  state: FloatBarState,
  nativeFloatBarFactory: (ComposeWindow) -> NativeMagnetFloatBar,
) {
  // TODO 将拖动反应到窗口位置上
  val parentWindow by taskbarController.desktopController.viewController.asDesktop()
    .composeWindowAsState()
  val nativeFloatBar = remember(parentWindow) {
    nativeFloatBarFactory(parentWindow)
  }

  SideEffect {
    nativeFloatBar.isVisible = true
  }

  DisposableEffect(Unit) {
    onDispose {
      nativeFloatBar.dispose()
    }
  }

  val layoutWidth by state.layoutWidthFlow.collectAsState()
  val layoutHeight by state.layoutHeightFlow.collectAsState()
  val dragging by state.draggingFlow.collectAsState()
  LaunchedEffect(layoutWidth, layoutHeight) {
    nativeFloatBar.setSize(layoutWidth.toInt(), layoutHeight.toInt())
    if (!nativeFloatBar.dragging) {
      nativeFloatBar.playMagnetEffect()
    }
  }
  nativeFloatBar.dragging = dragging
}