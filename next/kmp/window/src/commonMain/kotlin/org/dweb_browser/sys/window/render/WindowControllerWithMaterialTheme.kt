package org.dweb_browser.sys.window.render

import androidx.compose.runtime.Composable
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.constant.WindowColorScheme
import org.dweb_browser.sys.window.core.constant.WindowPropertyField
import org.dweb_browser.sys.window.helper.watchedState

/**
 * 提供一个计算函数，来获得一个在Compose中使用的 state
 */
@Composable
fun WindowController.WithMaterialTheme(content: @Composable () -> Unit) {
  DwebBrowserAppTheme(isDarkTheme = watchedState(WindowPropertyField.ColorScheme) {
    when (colorScheme) {
      WindowColorScheme.Normal -> null
      WindowColorScheme.Light -> false
      WindowColorScheme.Dark -> true
    }
  }.value, content = content)
}