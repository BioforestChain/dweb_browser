package org.dweb_browser.sys.window.render

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
    /// 键盘的互操作性
    EffectKeyboard()
    /// 底部导航栏的互操作
    EffectNavigationBar()
    /// 窗口截屏安全限制
    EffectSafeModel()
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

fun WindowController.openPvcInNativeWindow(pvc: PureViewController) {
  pvc.lifecycleScope.launch {
    pvc.composeWindow.openWindow()
  }
}

@Composable
fun RenderWindowInNative(
  windowsManager: WindowsManager<*>,
  win: WindowController,
) {
  val compositionChain = rememberUpdatedState(LocalCompositionChain.current)
  val pvc =
    win.getDesktopWindowNativeView(windowsManager, compositionChain).pvc

  /// 启动
  DisposableEffect(pvc) {
    win.openPvcInNativeWindow(pvc)
    val off = win.onClose {
      pvc.composeWindow.closeWindow()
    }
    onDispose {
      off()
    }
  }
}

private fun WindowController.getDesktopWindowNativeView(
  windowsManager: WindowsManager<*>,
  compositionChain: State<CompositionChain>
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
        BoxWithConstraints {
          val compositionChain by params["compositionChain"] as State<CompositionChain>
          compositionChain.Provider(LocalCompositionChain.current)
            .Provider(LocalWindowsManager provides windowsManager) {
              /// 渲染窗口
              win.Render(
                modifier = Modifier.windowImeOutsetBounds(),
                maxWinWidth = maxWidth.value,
                maxWinHeight = maxHeight.value
              )
            }
        }
      }
    }
  }

  companion object {
    val INSTANCES = WeakHashMap<WindowController, DesktopWindowNativeView>()
  }
}