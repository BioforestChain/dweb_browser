package org.dweb_browser.browser.web

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.dweb_browser.browser.web.ui.browser.BrowserViewForWindow
import org.dweb_browser.sys.window.core.WindowRenderScope
import org.dweb_browser.sys.window.render.LocalWindowController

@Composable
fun BrowserController.Render(modifier: Modifier, windowRenderScope: WindowRenderScope) {
  val controller = this
  val win = LocalWindowController.current
  val scope = rememberCoroutineScope()

  controller.viewModel.uiState.currentBrowserBaseView.value?.viewItem?.let { viewItem ->
    key(viewItem) {
      val canGoBack = if(win.isMaximized()) true else viewItem.navigator.canGoBack
      win.GoBackHandler(canGoBack) {
        if (viewItem.navigator.canGoBack) {
          viewItem.navigator.navigateBack()
        } else {
          scope.launch { win.close() }
        }
      }
    }
  }

  Box(modifier = modifier) {
    BrowserViewForWindow(controller.viewModel, modifier, windowRenderScope)
  }
}