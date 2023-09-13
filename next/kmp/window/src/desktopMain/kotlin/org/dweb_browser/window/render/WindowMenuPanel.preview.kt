package org.dweb_browser.window.render

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.dweb_browser.window.core.constant.WindowColorScheme
import org.dweb_browser.window.core.constant.WindowMode


@Preview
@Composable
fun PreviewWindowMenuPanel() {
  WindowPreviewer(config = {
    state.showMenuPanel = true
    state.mode = WindowMode.MAXIMIZE
  }) { modifier ->
    Box(modifier) {
      val win = LocalWindowController.current
      val showMenuPanel by win.watchedState { showMenuPanel }
      Text(
        text = "preview menu panel: $showMenuPanel",
        modifier = Modifier.align(Alignment.Center)
      )
    }
  }
}

@Preview
@Composable
fun PreviewWindowMenuPanelLight() {
  WindowPreviewer(config = {
    state.topBarContentColor = "#FF00FF"
    state.themeColor = "#Fd9F9F"
    state.showMenuPanel = true
    state.colorScheme = WindowColorScheme.Light
  }) {
    val win = LocalWindowController.current;
    WindowControlPanel(win, modifier = Modifier.fillMaxSize())
  }
}
@Preview
@Composable
fun PreviewWindowMenuPanelDark() {
  WindowPreviewer(config = {
    state.topBarContentColor = "#FF00FF"
    state.themeColor = "#Fd9F9F"
    state.showMenuPanel = true
    state.colorScheme = WindowColorScheme.Dark
  }) {
    val win = LocalWindowController.current;
    WindowControlPanel(win, modifier = Modifier.fillMaxSize())
  }
}