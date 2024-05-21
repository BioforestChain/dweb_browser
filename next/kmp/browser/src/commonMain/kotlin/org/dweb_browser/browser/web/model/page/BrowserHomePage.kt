package org.dweb_browser.browser.web.model.page

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.ui.page.BrowserHomePageRender

class BrowserHomePage(browserController: BrowserController) : BrowserPage(browserController) {
  companion object {
    fun isNewTabUrl(url: String) = url.isEmpty() || BrowserPageType.Home.isMatchUrl(url)
  }

  override val icon
    @Composable get() = BrowserPageType.Home.iconPainter() // BrowserDrawResource.Star.painter()
  override val iconColorFilter
    @Composable get() = ColorFilter.tint(LocalContentColor.current)// BrowserDrawResource.Star.getContentColorFilter()

  init {
    url = BrowserPageType.Home.url // "about:newtab"
  }

  override fun isUrlMatch(url: String) = isNewTabUrl(url)

  @Composable
  override fun Render(modifier: Modifier) {
    title = BrowserI18nResource.Home.page_title()
    BrowserHomePageRender(modifier)
  }

  override suspend fun destroy() {
  }

  val browserPageList = listOf(
    BrowserPageType.Bookmark,
    BrowserPageType.History,
    BrowserPageType.Engine,
    BrowserPageType.Download
  )
}

