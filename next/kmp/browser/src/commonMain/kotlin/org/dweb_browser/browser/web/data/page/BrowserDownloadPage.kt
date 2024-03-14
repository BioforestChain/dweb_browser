package org.dweb_browser.browser.web.data.page

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.web.BrowserController

class BrowserDownloadPage(browserController: BrowserController) : BrowserPage(browserController) {
  companion object {
    fun isDownloadUrl(url: String) = isAboutPage(url, "downloads")
  }

  override fun isUrlMatch(url: String) = isDownloadUrl(url)

  @Composable
  override fun Render(modifier: Modifier) {

  }

  override suspend fun destroy() {
  }
}