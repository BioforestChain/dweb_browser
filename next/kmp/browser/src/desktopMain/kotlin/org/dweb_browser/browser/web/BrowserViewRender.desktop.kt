package org.dweb_browser.browser.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.ui.BrowserViewModalRender
import org.dweb_browser.helper.platform.LocalPureViewController
import org.dweb_browser.helper.platform.asDesktop
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.watchedState

@Composable
actual fun CommonBrowserView(
  viewModel: BrowserViewModel,
  modifier: Modifier,
  windowRenderScope: WindowContentRenderScope
) {
  BrowserViewModalRender(viewModel, modifier, windowRenderScope)
  // TODO 临时方案
  val composeWindow by LocalPureViewController.current.asDesktop().composeWindowAsState()
  LaunchedEffect(viewModel.showMore, composeWindow) {
    while (true) {
      if (viewModel.showMore && composeWindow.focusOwner != null) {
        viewModel.showMore = false
      }
      delay(100)
    }
  }
  val windowController = LocalWindowController.current
  val isShowMenuPanel by windowController.watchedState { showMenuPanel }
  LaunchedEffect(isShowMenuPanel, composeWindow) {
    while (true) {
      if (isShowMenuPanel && composeWindow.focusOwner != null) {
        windowController.hideMenuPanel()
      }
      delay(100)
    }
  }
}