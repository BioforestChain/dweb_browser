package org.dweb_browser.sys.window.render

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.compose.CompositionChain
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager
import org.dweb_browser.sys.window.core.WindowsManagerState.Companion.windowImeOutsetBounds
import org.dweb_browser.sys.window.core.constant.debugWindow

@Composable
actual fun <T : WindowController> WindowsManager<T>.Render() {
  val windowsManager = this
  BoxWithConstraints {
    WindowsManagerEffect()
    /// 普通层级的窗口
    debugWindow("WindowsManager.Render", "winList: ${winList.size}")
    for (win in winList) {
      key(win.id) {
        RenderWindowInNative(windowsManager, win)
      }
    }
    /// 置顶层级的窗口
    debugWindow("WindowsManager.Render", "winListTop: ${winListTop.size}")
    for (win in winListTop) {
      key(win.id) {
        RenderWindowInNative(windowsManager, win)
      }
    }
  }
}

@Composable
fun RenderWindowInNative(
  windowsManager: WindowsManager<*>,
  win: WindowController,
) {
  val compositionChain = rememberUpdatedState(LocalCompositionChain.current)
  val pvc = win.getDesktopWindowNativeView(windowsManager, compositionChain).pvc

  val uiScope = rememberCoroutineScope()
  /// 启动
  DisposableEffect(pvc) {
    val job = uiScope.launch {
      if (!pvc.composeWindowParams.isOpened) {
        // 使用系统原生的窗口进行渲染
        win.state.renderConfig.useSystemFrame = true

        pvc.composeWindowParams.apply {
          // 关闭边框
          undecorated = true
          // 背景透明，为了调试，展示不透明
          transparent = true
          // 打开窗口
          openWindow()
        }
      }
    }
    val off = win.onClose {
      pvc.composeWindowParams.closeWindow()
    }
    onDispose {
      job.cancel()
      off()
    }
  }
}

private fun WindowController.getDesktopWindowNativeView(
  windowsManager: WindowsManager<*>, compositionChain: State<CompositionChain>
) = DesktopWindowNativeView.INSTANCES.getOrPut(this) {
  DesktopWindowNativeView(
    mutableMapOf(
      "compositionChain" to compositionChain,
    ), this, windowsManager
  )
}

private class DesktopWindowNativeView(
  params: Map<String, Any?>,
  private val win: WindowController,
  private val windowsManager: WindowsManager<*>
) {
  val pvc = PureViewController(params).also { pvc ->
    pvc.onCreate { params ->
      @Suppress("UNCHECKED_CAST") pvc.addContent {
        val composeWindow by pvc.composeWindowAsState()
        val screenSize = composeWindow.toolkit.screenSize
        val compositionChain by params["compositionChain"] as State<CompositionChain>
        compositionChain.Provider(LocalCompositionChain.current)
          .Provider(LocalWindowsManager provides windowsManager) {
            /// 注册副作用
            win.WindowControllerEffect()
            /// 渲染窗口
            win.Render(
              modifier = Modifier.windowImeOutsetBounds(),
              winMaxWidth = screenSize.width.toFloat(),
              winMaxHeight = screenSize.height.toFloat(),
              minScale = 1.0
            )
          }
      }
    }
  }

  companion object {
    val INSTANCES = WeakHashMap<WindowController, DesktopWindowNativeView>()
  }
}