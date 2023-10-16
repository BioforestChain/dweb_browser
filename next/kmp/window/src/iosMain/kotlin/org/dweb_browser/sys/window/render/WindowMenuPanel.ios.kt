package org.dweb_browser.sys.window.render

import androidx.compose.runtime.Composable
import org.dweb_browser.sys.window.core.WindowController

@Composable
internal actual fun WindowMenuPanel(win: WindowController) {
  WindowMenuPanelByAlert(win)
}