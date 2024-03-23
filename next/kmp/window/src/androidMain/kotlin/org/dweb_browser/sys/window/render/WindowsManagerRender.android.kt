package org.dweb_browser.sys.window.render

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager
import org.dweb_browser.sys.window.core.WindowsManagerState.Companion.windowImeOutsetBounds
import org.dweb_browser.sys.window.core.constant.debugWindow

@Composable
actual fun <T : WindowController> WindowsManager<T>.Render() {
  LocalCompositionChain.current.Provider(
    LocalWindowsManager provides this,
  ) {
    BoxWithConstraints {
      WindowsManagerEffect()

      /// 普通层级的窗口
      debugWindow("WindowsManager.Render", "winList: ${winList.size}")
      for (win in winList) {
        key(win.id) {
          /// 渲染窗口
          win.Render(
            modifier = Modifier
              .zIndex(win.state.zIndex.toFloat())
              .windowImeOutsetBounds(),
            winMaxWidth = maxWidth.value,
            winMaxHeight = maxHeight.value
          )
        }
      }
      /// 置顶层级的窗口
      debugWindow("WindowsManager.Render", "winListTop: ${winListTop.size}")
      for (win in winListTop) {
        key(win.id) {
          /// 渲染窗口
          win.MaterialTheme {
            win.Render(
              modifier = Modifier
                .zIndex(winList.size + win.state.zIndex.toFloat())
                .windowImeOutsetBounds(),
              winMaxWidth = maxWidth.value,
              winMaxHeight = maxHeight.value
            )
          }
        }
      }
    }
  }
}
