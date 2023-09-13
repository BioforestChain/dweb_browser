package org.dweb_browser.browserUI.microService.browser.web

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
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

  val navigator = controller.viewModel.uiState.currentBrowserBaseView.value.viewItem.navigator
  win.state.canGoBack = navigator.canGoBack || localCommonUrl.value.isNotEmpty()
  win.GoBackHandler {
    if (localCommonUrl.value.isNotEmpty()) {
      localCommonUrl.value = ""
    } else {
      navigator.navigateBack()
    }
  }

  Box(modifier = modifier) {
    BrowserViewForWindow(
      controller.viewModel,
      modifier,
      windowRenderScope,
    )
  }
}