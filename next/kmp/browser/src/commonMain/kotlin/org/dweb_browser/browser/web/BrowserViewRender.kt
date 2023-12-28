package org.dweb_browser.browser.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.sys.window.core.WindowRenderScope

@Composable
fun BrowserController.Render(modifier: Modifier, windowRenderScope: WindowRenderScope) {
  CommonBrowserView(this.viewModel, modifier, windowRenderScope)
}

@Composable
expect fun CommonBrowserView(
  viewModel: BrowserViewModel,
  modifier: Modifier,
  windowRenderScope: WindowRenderScope
)

