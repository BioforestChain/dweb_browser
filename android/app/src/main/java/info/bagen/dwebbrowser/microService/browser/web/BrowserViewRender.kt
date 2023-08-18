package info.bagen.dwebbrowser.microService.browser.web

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.browserUI.ui.browser.BrowserViewForWindow
import org.dweb_browser.browserUI.ui.loading.LoadingView
import org.dweb_browser.window.core.WindowRenderScope

@Composable
fun BrowserController.Render(modifier: Modifier, windowRenderScope: WindowRenderScope) {
  val controller = this
  Box(modifier = modifier) {
    BrowserViewForWindow(
      controller.viewModel,
      modifier,
      windowRenderScope,
    )
    LoadingView(showLoading)
  }
}