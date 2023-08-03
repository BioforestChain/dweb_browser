package info.bagen.dwebbrowser.microService.browser.desk.view

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import info.bagen.dwebbrowser.microService.browser.desk.DesktopWindowsManager
import info.bagen.dwebbrowser.microService.browser.desk.view.Render

@Composable
fun DesktopWindowsManager.Render() {
  val systemUiController = rememberSystemUiController()
  BoxWithConstraints {
    var hasMaximized by remember { mutableStateOf(false) }
    for (win in winList.value) {
      key(win.id) {
        /// 渲染窗口
        win.Render(
          modifier = Modifier.zIndex(win.state.zIndex.toFloat()),
          maxWinWidth = maxWidth.value,
          maxWinHeight = maxHeight.value
        )
        if (win.isMaximized()) {
          hasMaximized = true
        }
      }
    }
    /// 如果有窗口处于全屏模式，将操作系统的导航栏标记为隐藏，反之显示
    systemUiController.isNavigationBarVisible = !hasMaximized;
    println("hasMaximized: $hasMaximized")
  }
}
