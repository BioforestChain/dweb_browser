package org.dweb_browser.browser.web.data.page

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.web.BrowserController

class BrowserSettingPage(browserController: BrowserController) : BrowserPage(browserController) {
  companion object {
    fun isSettingUrl(url: String) = isAboutPage(url, "settings")
  }

  override fun isUrlMatch(url: String) = isSettingUrl(url)

  @Composable
  override fun Render(modifier: Modifier) {

  }

  override suspend fun destroy() {
  }
}