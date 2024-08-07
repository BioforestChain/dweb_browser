package org.dweb_browser.sys.window.render

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowRenderConfig
import org.dweb_browser.sys.window.core.WindowsManager
import org.dweb_browser.sys.window.core.WindowsManagerState.Companion.windowImeOutsetBounds
import org.dweb_browser.sys.window.core.constant.debugWindow

@Composable
actual fun <T : WindowController> WindowsManager<T>.SceneRender(modifier: Modifier) {
  LocalCompositionChain.current.Provider(
    LocalWindowsManager provides this,
  ) {
    BoxWithConstraints(modifier) {
      WindowsManagerEffect()

      /// 普通层级的窗口
      val winList by winListFlow.collectAsState()
      debugWindow("WindowsManager.Render", "winList: ${winList.size}")
      for (win in winList) {
        key(win.id) {
          /// 渲染窗口
          AndroidWindowPrepare(
            windowsManager = this@SceneRender,
            win = win,
            sceneMaxWidth = maxWidth.value,
            sceneMaxHeight = maxHeight.value,
            zIndexBase = 0,
          )
        }
      }
      /// 置顶层级的窗口
      val topWinList by topWinListFlow.collectAsState()
      debugWindow("WindowsManager.Render", "winListTop: ${topWinList.size}")
      for (win in topWinList) {
        key(win.id) {
          /// 渲染窗口
          AndroidWindowPrepare(
            windowsManager = this@SceneRender,
            win = win,
            sceneMaxWidth = maxWidth.value,
            sceneMaxHeight = maxHeight.value,
            zIndexBase = winList.size + 1000
          )
        }
      }
    }
  }
}

@Composable
private fun AndroidWindowPrepare(
  windowsManager: WindowsManager<*>,
  win: WindowController,
  sceneMaxWidth: Float,
  sceneMaxHeight: Float,
  zIndexBase: Int,
) {
  /// 渲染窗口
  win.Prepare(
    winMaxWidth = sceneMaxWidth,
    winMaxHeight = sceneMaxHeight,
  ) {
    /// 渲染窗口
    win.WithMaterialTheme {
      win.WindowRender(
        modifier = Modifier
          .zIndex(win.watchedState { (zIndexBase + zIndex).toFloat() }.value)
          .windowImeOutsetBounds()
      )
    }

    /// 响应窗口的拖动行为
    val safeBounds = win.safeBounds(LocalWindowLimits.current)
    val density = LocalDensity.current.density
    LaunchedEffect(win) {
      win.state.renderConfig.frameDragDelegate =
        WindowRenderConfig.FrameDragDelegate(
          onMove = { _, dragAmount ->
            win.state.updateBounds {
              moveWindowBoundsInSafeBounds(this, safeBounds, (dragAmount / density))
            }
          },
        )
    }
  }
}