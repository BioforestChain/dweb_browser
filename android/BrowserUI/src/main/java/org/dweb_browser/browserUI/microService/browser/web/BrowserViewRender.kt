package org.dweb_browser.browserUI.microService.browser.web

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.browserUI.ui.browser.BrowserViewForWindow
import org.dweb_browser.browserUI.ui.loading.LoadingView
import org.dweb_browser.window.core.WindowRenderScope
import org.dweb_browser.window.render.LocalWindowController

@Composable
fun BrowserController.Render(modifier: Modifier, windowRenderScope: WindowRenderScope) {
  val controller = this
  val win = LocalWindowController.current
  controller.viewModel.uiState.currentBrowserBaseView.value.viewItem.navigator.apply {
    win.state.canGoBack = this.canGoBack
    // win.state.canGoForward = this.canGoForward
    win.GoBackHandler {
      this.navigateBack()
    }
  }

  Box(modifier = modifier) {
    BrowserViewForWindow(
      controller.viewModel,
      modifier,
      windowRenderScope,
    )
    LoadingView(showLoading)
  }
}