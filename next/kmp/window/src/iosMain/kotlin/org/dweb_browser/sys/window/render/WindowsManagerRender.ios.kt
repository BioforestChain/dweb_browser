package org.dweb_browser.sys.window.render

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
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
  private val windowsManager: WindowsManager<*>,
) {
  val pvc = PureViewController(params).also { pvc ->
    pvc.onCreate { params ->
      @Suppress("UNCHECKED_CAST") pvc.addContent {
        val compositionChain by params["compositionChain"] as State<CompositionChain>
        compositionChain.Provider(LocalCompositionChain.current)
          .Provider(LocalWindowsManager provides windowsManager) {
            /// 渲染窗口
            win.MaterialTheme {
              win.WindowRender(Modifier.windowImeOutsetBounds())
            }
          }
      }
    }
  }

  companion object {
    private val INSTANCES = WeakHashMap<WindowController, IosWindowNativeView>()
    fun from(
      win: WindowController,
      windowsManager: WindowsManager<*>,
      compositionChain: State<CompositionChain>,
    ) = IosWindowNativeView.INSTANCES.getOrPut(win) {
      IosWindowNativeView(
        mutableMapOf(
          "compositionChain" to compositionChain,
        ), win, windowsManager
      )
    }
  }
}

@Composable
private fun RenderWindowInNewLayer(
  windowsManager: WindowsManager<*>,
  win: WindowController,
  sceneMaxWidth: Float,
  sceneMaxHeight: Float,
  zIndexBase: Int,
) {
  win.Prepare(
    winMaxWidth = sceneMaxWidth,
    winMaxHeight = sceneMaxHeight,
  ) {
    val pvc = IosWindowNativeView.from(
      win = win,
      windowsManager = windowsManager,
      compositionChain = rememberUpdatedState(LocalCompositionChain.current)
    ).pvc

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
}

@Composable
actual fun <T : WindowController> WindowsManager<T>.SceneRender() {
  val windowsManager = this
  BoxWithConstraints {
    WindowsManagerEffect()
    /// 普通层级的窗口
    val winList by winListFlow.collectAsState()
    debugWindow("WindowsManager.Render", "winList: ${winList.size}")
    for (win in winList) {
      key(win.id) {
        RenderWindowInNewLayer(windowsManager, win, maxWidth.value, maxHeight.value, 10)
      }
    }
    /// 置顶层级的窗口
    val topWinList by topWinListFlow.collectAsState()
    debugWindow("WindowsManager.Render", "winListTop: ${topWinList.size}")
    for (win in topWinList) {
      key(win.id) {
        RenderWindowInNewLayer(windowsManager, win, maxWidth.value, maxHeight.value, 10000)
      }
    }
  }
}