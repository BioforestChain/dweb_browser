package info.bagen.dwebbrowser.microService.browser.desk.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import org.dweb_browser.window.core.constant.WindowMode
import org.dweb_browser.window.render.LocalWindowController
import org.dweb_browser.window.render.WindowPreviewer
import org.dweb_browser.window.render.watchedState

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

@Preview(widthDp = 400, heightDp = 160)
@Composable
fun PreviewWindowTopBar() {
  WindowPreviewer(config = {
    state.topBarContentColor = "#FF00FF"
    state.iconUrl = "http://172.30.92.50:12207/m3-favicon-apple-touch.png"
    state.iconMaskable = true
  }) { modifier, _, _, _ ->
    PreviewWindowTopBarContent(modifier)
  }
}

@Preview(widthDp = 400, heightDp = 160)
@Composable
fun PreviewWindowTopBarWithMax() {
  WindowPreviewer(config = {
    state.mode = WindowMode.MAXIMIZE
    state.topBarContentColor = "#FF00FF"
  }) { modifier, _, _, _ ->
    PreviewWindowTopBarContent(modifier)
  }
}


@Preview(widthDp = 400, heightDp = 160)
@Composable
fun PreviewWindowTopBarWithLongTitle() {
  WindowPreviewer(config = {
    state.topBarContentColor = "#FF00FF"
    state.title = "🎉🎉🎉🎉 Hello Dweb Browser!!Hello Dweb Browser 🎉🎉🎉🎉"
  }) { modifier, _, _, _ ->
    PreviewWindowTopBarContent(modifier)
  }
}