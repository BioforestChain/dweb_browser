package info.bagen.dwebbrowser.microService.browser.desk.view

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.core.view.WindowInsetsControllerCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import info.bagen.dwebbrowser.microService.browser.desk.DesktopWindowsManager

@Composable
fun DesktopWindowsManager.Render() {
  val systemUiController = rememberSystemUiController()
  systemUiController.systemBarsBehavior =
    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
  BoxWithConstraints {
    SideEffect {
      hasMaximizedWins.onChange {
        val hasMaximized = it.size > 0;
        /// 如果有窗口处于全屏模式，将操作系统的导航栏标记为隐藏，反之显示
        systemUiController.isNavigationBarVisible = !hasMaximized
        println("hasMaximized: $hasMaximized")
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
