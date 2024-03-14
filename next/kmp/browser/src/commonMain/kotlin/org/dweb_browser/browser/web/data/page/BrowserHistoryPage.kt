package org.dweb_browser.browser.web.data.page

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.web.BrowserController

class BrowserHistoryPage(browserController: BrowserController) : BrowserPage(browserController) {
  companion object {
    fun isHistoryUrl(url: String) = isAboutPage(url, "history")
  }

  override fun isUrlMatch(url: String) = isHistoryUrl(url)

  @Composable
  override fun Render(modifier: Modifier) {

  }

  override suspend fun destroy() {
  }
}