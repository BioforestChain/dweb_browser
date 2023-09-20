package org.dweb_browser.browserUI.microService.browser.web

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import org.dweb_browser.browserUI.ui.browser.BrowserViewForWindow
import org.dweb_browser.browserUI.ui.view.LocalCommonUrl
import org.dweb_browser.window.core.WindowRenderScope
import org.dweb_browser.window.render.LocalWindowController

@Composable
fun BrowserController.Render(modifier: Modifier, windowRenderScope: WindowRenderScope) {
  val controller = this
  val win = LocalWindowController.current
  val localCommonUrl = LocalCommonUrl.current

  val viewModel = controller.viewModel
  val viewItem = viewModel.uiState.currentBrowserBaseView.value.viewItem
  key(viewItem) {
    win.state.canGoBack = viewItem.navigator.canGoBack || localCommonUrl.value.isNotEmpty()
    win.GoBackHandler {
      if (localCommonUrl.value.isNotEmpty()) {
        localCommonUrl.value = ""
      } else {
        viewItem.navigator.navigateBack()
      }
    }
  }
  Box(modifier = modifier) { BrowserViewForWindow(viewModel, modifier, windowRenderScope) }
}