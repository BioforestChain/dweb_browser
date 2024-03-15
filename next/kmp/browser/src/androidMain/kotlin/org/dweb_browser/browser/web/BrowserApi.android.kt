package org.dweb_browser.browser.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.ui.BrowserViewModalRender
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.sys.window.core.WindowRenderScope

actual fun getImageResourceRootPath(): String {
  return getAppContext().filesDir.absolutePath + "/icons"
}

@Composable
actual fun CommonBrowserView(
  viewModel: BrowserViewModel, modifier: Modifier, windowRenderScope: WindowRenderScope
) {
  BrowserViewModalRender(viewModel, modifier, windowRenderScope)
}