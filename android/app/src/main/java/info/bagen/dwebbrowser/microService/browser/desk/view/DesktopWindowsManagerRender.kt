package info.bagen.dwebbrowser.microService.browser.desk.view

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.core.view.WindowInsetsControllerCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import info.bagen.dwebbrowser.microService.browser.desk.DesktopWindowsManager
import info.bagen.dwebbrowser.microService.browser.desk.debugDesk

@Composable
fun DesktopWindowsManager.Render() {
  val systemUiController = rememberSystemUiController()
  BoxWithConstraints {
    DisposableEffect(hasMaximizedWins) {
      systemUiController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

      debugDesk("DesktopWindowsManager.Render", "start watch maximize")
      val off = hasMaximizedWins.watch {
        val hasMaximized = it.size > 0;
        /// 如果有窗口处于全屏模式，将操作系统的导航栏标记为隐藏，反之显示
        systemUiController.isNavigationBarVisible = !hasMaximized
        debugDesk("hasMaximized", hasMaximized)
      }
      onDispose {
        debugDesk("DesktopWindowsManager.Render", "stop watch maximize")
        off()
      }
    }

    for (win in winList.value) {
      key(win.id) {
        /// 渲染窗口
        win.Render(
          modifier = Modifier.zIndex(win.state.zIndex.toFloat()),
          maxWinWidth = maxWidth.value,
          maxWinHeight = maxHeight.value
        )
      }
    }
  }
}
