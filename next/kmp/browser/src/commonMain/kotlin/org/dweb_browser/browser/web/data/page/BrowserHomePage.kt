package org.dweb_browser.browser.web.data.page

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.ui.page.BrowserHomePageRender

class BrowserHomePage(browserController: BrowserController) : BrowserPage(browserController) {
  companion object {
    fun isNewTabUrl(url: String) = url.isEmpty() || isAboutPage(url, "newtab")
  }

  override fun isUrlMatch(url: String) = isNewTabUrl(url)

  @Composable
  override fun Render(modifier: Modifier) {
    BrowserHomePageRender(modifier)
  }

  override suspend fun destroy() {
  }
}