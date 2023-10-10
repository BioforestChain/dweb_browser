package org.dweb_browser.sys.window.render

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import org.dweb_browser.sys.window.core.ManagerState.Companion.windowImeOutsetBounds
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager
import org.dweb_browser.sys.window.core.constant.debugWindow
import org.dweb_browser.sys.window.render.EffectKeyboard
import org.dweb_browser.sys.window.render.EffectNavigationBar

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T : WindowController> WindowsManager<T>.Render() {
  CompositionLocalProvider(
    LocalWindowsManager provides this,
  ) {
    BoxWithConstraints {
      /// 键盘的互操作性
      EffectKeyboard()
      /// 底部导航栏的互操作
      EffectNavigationBar()

      /// 普通层级的窗口
      val winList by winList
      debugWindow("WindowsManager.Render", "winList: ${winList.size}")
      for (win in winList) {
        key(win.id) {
          /// 渲染窗口
          win.Render(
            modifier = Modifier
              .zIndex(win.state.zIndex.toFloat())
              .windowImeOutsetBounds(),
            maxWinWidth = maxWidth.value,
            maxWinHeight = maxHeight.value
          )
        }
      }
      /// 置顶层级的窗口
      val winListTop by winListTop
      debugWindow("WindowsManager.Render", "winListTop: ${winListTop.size}")
      for (win in winListTop) {
        key(win.id) {
          /// 渲染窗口
          win.Render(
            modifier = Modifier
              .zIndex(winList.size + win.state.zIndex.toFloat())
              .windowImeOutsetBounds(),
            maxWinWidth = maxWidth.value,
            maxWinHeight = maxHeight.value
          )
        }
      }
    }
  }
}
