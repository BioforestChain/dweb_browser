package org.dweb_browser.browser.web.model.page

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import org.dweb_browser.browser.BrowserDrawResource
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.ui.page.BrowserHomePageRender

class BrowserHomePage(browserController: BrowserController) : BrowserPage(browserController) {
  companion object {
    fun isNewTabUrl(url: String) = url.isEmpty() || isAboutPage(url, "newtab")
  }

  override val icon
    @Composable get() = BrowserDrawResource.Star.painter()
  override val iconColorFilter
    @Composable get() = BrowserDrawResource.Star.getContentColorFilter()

  init {
    url = "about:newtab"
  }

  override fun isUrlMatch(url: String) = isNewTabUrl(url)

  @Composable
  override fun Render(modifier: Modifier) {
    title = BrowserI18nResource.Home.page_title()
    BrowserHomePageRender(modifier.scale(scale))
  }

  override suspend fun destroy() {
  }
}