package org.dweb_browser.browser.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.DwebLinkSearchItem
import org.dweb_browser.browser.web.ui.BrowserViewModalRender
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.sys.window.core.WindowContentRenderScope

actual fun getImageResourceRootPath(): String {
  return getAppContext().filesDir.absolutePath + "/icons"
}

@Composable
actual fun CommonBrowserView(
  viewModel: BrowserViewModel, modifier: Modifier, windowRenderScope: WindowContentRenderScope
) {
  BrowserViewModalRender(viewModel, modifier, windowRenderScope)
}

actual suspend fun deepLinkDoSearch(dwebLinkSearchItem: DwebLinkSearchItem) {
  // 不需要实现，这个目前是专门给 Ios 使用
}