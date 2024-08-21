package org.dweb_browser.browser.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.sys.window.core.LocalWindowController
import org.dweb_browser.sys.window.helper.LocalWindowControllerTheme
import org.dweb_browser.sys.window.helper.watchedState

@Composable
fun IDWebView.WindowControllerBinding() {
  val win = LocalWindowController.current
  val colorScheme by win.watchedState { colorScheme }
  /// 绑定暗色模式
  LaunchedEffect(colorScheme) {
    setPrefersColorScheme(colorScheme.toWebColorScheme())
  }
  /// 绑定默认背景色
  val themeColor = LocalWindowControllerTheme.current.themeColor
  LaunchedEffect(themeColor) {
    backgroundColor = themeColor
  }
}