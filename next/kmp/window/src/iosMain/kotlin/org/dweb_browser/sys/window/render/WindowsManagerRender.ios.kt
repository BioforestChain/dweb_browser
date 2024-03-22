package org.dweb_browser.sys.window.render

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.compose.CompositionChain
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager
import org.dweb_browser.sys.window.core.WindowsManagerState.Companion.windowImeOutsetBounds
import org.dweb_browser.sys.window.core.constant.debugWindow


private class IosWindowNativeView(
  params: Map<String, Any?>,
  private val win: WindowController,
  private val windowsManager: WindowsManager<*>
) {
  val pvc = PureViewController(params).also { pvc ->
    pvc.onCreate { params ->
      @Suppress("UNCHECKED_CAST") pvc.addContent {
        val maxWidth by params["maxWidth"] as State<Float>
        val maxHeight by params["maxHeight"] as State<Float>
        val compositionChain by params["compositionChain"] as State<CompositionChain>
        compositionChain.Provider(LocalCompositionChain.current)
          .Provider(LocalWindowsManager provides windowsManager) {
            /// 渲染窗口
            win.Render(
              modifier = Modifier.windowImeOutsetBounds(),
              winMaxWidth = maxWidth,
              winMaxHeight = maxHeight
            )
          }
      }
    }
  }

  companion object {
    val INSTANCES = WeakHashMap<WindowController, IosWindowNativeView>()
  }
}

private fun WindowController.getIosWindowNativeView(
  windowsManager: WindowsManager<*>,
  maxWidth: State<Float>,
  maxHeight: State<Float>,
  compositionChain: State<CompositionChain>
) = IosWindowNativeView.INSTANCES.getOrPut(this) {
  IosWindowNativeView(
    mutableMapOf(
      "maxWidth" to maxWidth,
      "maxHeight" to maxHeight,
      "compositionChain" to compositionChain,
    ), this, windowsManager
  )
}

@Composable
private fun RenderWindowInNewLayer(
  windowsManager: WindowsManager<*>,
  win: WindowController,
  currentMaxWidth: Float,
  currentMaxHeight: Float,
  zIndexBase: Int
) {
  val maxWidth = rememberUpdatedState(currentMaxWidth)
  val maxHeight = rememberUpdatedState(currentMaxHeight)
  val compositionChain = rememberUpdatedState(LocalCompositionChain.current)
  val pvc = win.getIosWindowNativeView(windowsManager, maxWidth, maxHeight, compositionChain).pvc

  val zIndex by win.watchedState(zIndexBase) { zIndex + zIndexBase }
  // 防止Composable生命周期溢出
  LaunchedEffect(pvc) {
    nativeViewController.addOrUpdate(pvc, zIndex)
  }
  /// 启动
  DisposableEffect(pvc) {
    val off = win.onClose {
      nativeViewController.remove(pvc)
    }
    onDispose {
      off()
    }
  }
  /// 切换zIndex
  LaunchedEffect(zIndex) {
    nativeViewController.addOrUpdate(pvc, zIndex)
  }
}

@Composable
actual fun <T : WindowController> WindowsManager<T>.Render() {
  val windowsManager = this
  BoxWithConstraints {
    WindowsManagerEffect()
    /// 普通层级的窗口
    debugWindow("WindowsManager.Render", "winList: ${winList.size}")
    for (win in winList) {
      key(win.id) {
        RenderWindowInNewLayer(windowsManager, win, maxWidth.value, maxHeight.value, 10)
      }
    }
    /// 置顶层级的窗口
    debugWindow("WindowsManager.Render", "winListTop: ${winListTop.size}")
    for (win in winListTop) {
      key(win.id) {
        RenderWindowInNewLayer(windowsManager, win, maxWidth.value, maxHeight.value, 10000)
      }
    }
  }
}