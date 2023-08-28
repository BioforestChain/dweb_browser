package info.bagen.dwebbrowser.microService.browser.desk.view

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.dweb_browser.window.core.constant.WindowMode
import org.dweb_browser.window.render.LocalWindowController
import org.dweb_browser.window.render.WindowPreviewer
import org.dweb_browser.window.render.watchedState


@Preview(widthDp = 400, heightDp = 400)
@Composable
fun PreviewWindowMenuPanel() {
  WindowPreviewer(config = {
    state.showMenuPanel = true
    state.mode = WindowMode.MAXIMIZE
  }) { modifier ->
    Box(modifier) {
      val win = LocalWindowController.current
      val showMenuPanel by win.watchedState { showMenuPanel }
      Text(text = "preview menu panel: $showMenuPanel", modifier = Modifier.align(Alignment.Center))
    }
  }
}