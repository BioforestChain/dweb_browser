package org.dweb_browser.window.render

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.dweb_browser.window.core.constant.WindowMode

@Composable
fun PreviewWindowTopBarContent(modifier: Modifier) {
  Box(
    modifier
      .background(Color.DarkGray)
  ) {
    val iconUrl by LocalWindowController.current.watchedState { iconUrl ?: "" }
    TextField(iconUrl, onValueChange = {}, modifier = Modifier.fillMaxSize())
  }
}

@Preview
@Composable
fun PreviewWindowTopBar() {
  WindowPreviewer(config = {
    state.title = "应用长长的标题的标题的标题～～"
    state.topBarContentColor = "#FF00FF"
    state.themeColor = "#Fd9F9F"
    state.iconUrl = "http://172.30.92.50:12207/m3-favicon-apple-touch.png"
    state.iconMaskable = true
    state.showMenuPanel = true
  }) { modifier ->
    PreviewWindowTopBarContent(modifier)
  }
}

@Preview
@Composable
fun PreviewWindowTopBarWithMax() {
  WindowPreviewer(config = {
    state.mode = WindowMode.MAXIMIZE
    state.topBarContentColor = "#FF00FF"
  }) { modifier ->
    PreviewWindowTopBarContent(modifier)
  }
}


@Preview
@Composable
fun PreviewWindowTopBarWithLongTitle() {
  WindowPreviewer(config = {
    state.topBarContentColor = "#FF00FF"
    state.title = "🎉🎉🎉🎉 Hello Dweb Browser!!Hello Dweb Browser 🎉🎉🎉🎉"
  }) { modifier ->
    PreviewWindowTopBarContent(modifier)
  }
}