package org.dweb_browser.window.render

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.core.view.WindowInsetsControllerCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch
import org.dweb_browser.window.core.ManagerState.Companion.windowImeOutsetBounds
import org.dweb_browser.window.core.WindowController
import org.dweb_browser.window.core.WindowsManager
import org.dweb_browser.window.core.constant.debugWindow

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T : WindowController> WindowsManager<T>.Render() {
  val systemUiController = rememberSystemUiController()
  val scope = rememberCoroutineScope()
  CompositionLocalProvider(
    LocalWindowsManager provides this,
  ) {
    BoxWithConstraints {
      /// 底部导航栏的互操作
      DisposableEffect(hasMaximizedWins) {
        systemUiController.systemBarsBehavior =
          WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        debugWindow("WindowsManager.Render", "start watch maximize")
        val off = hasMaximizedWins.onChange {
          val hasMaximized = it.size > 0;
          /// 如果有窗口处于全屏模式，将操作系统的导航栏标记为隐藏，反之显示
          systemUiController.isNavigationBarVisible = !hasMaximized
          debugWindow("hasMaximized", hasMaximized)
        }
        scope.launch {
          off.emitSelf(hasMaximizedWins)
        }
        onDispose {
          debugWindow("WindowsManager.Render", "stop watch maximize")
          off()
        }
      }
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
