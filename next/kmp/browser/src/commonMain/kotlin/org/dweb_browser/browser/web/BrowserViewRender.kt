package org.dweb_browser.browser.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.ui.BrowserDownloadDialog
import org.dweb_browser.sys.window.core.WindowContentRenderScope

@Composable
fun BrowserController.Render(modifier: Modifier, windowRenderScope: WindowContentRenderScope) {
  CommonBrowserView(viewModel, modifier, windowRenderScope)
  downloadController.BrowserDownloadDialog()
}

@Composable
expect fun CommonBrowserView(
  viewModel: BrowserViewModel,
  modifier: Modifier,
  windowRenderScope: WindowContentRenderScope,
)

