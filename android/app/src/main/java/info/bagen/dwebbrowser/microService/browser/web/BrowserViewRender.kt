package info.bagen.dwebbrowser.microService.browser.web

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.browserUI.ui.browser.BrowserViewForWindow
import org.dweb_browser.browserUI.ui.loading.LoadingView

@Composable
fun BrowserController.Render(modifier: Modifier, width: Float, height: Float, scale: Float) {
  val controller = this
  Box(modifier = modifier) {
    BrowserViewForWindow(
      viewModel = controller.viewModel, modifier = modifier, width, height, scale
    )
    LoadingView(showLoading)
  }
}