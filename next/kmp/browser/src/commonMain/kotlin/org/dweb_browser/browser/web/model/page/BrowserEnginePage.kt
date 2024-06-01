package org.dweb_browser.browser.web.model.page

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.ui.page.BrowserEnginePageRender

class BrowserEnginePage(browserController: BrowserController) : BrowserPage(browserController) {
  companion object {
    fun isEngineUrl(url: String) =
      BrowserPageType.Engine.isMatchUrl(url) // isAboutPage(url, "engines")
  }

  override val icon
    @Composable get() = BrowserPageType.Engine.iconPainter() // rememberVectorPainter(Icons.TwoTone.PersonSearch)
  override val iconColorFilter
    @Composable get() = ColorFilter.tint(LocalContentColor.current)

  init {
    url = BrowserPageType.Engine.url // "about:engines"
  }

  override fun isUrlMatch(url: String) = isEngineUrl(url)

  @Composable
  override fun Render(modifier: Modifier) {
    title = BrowserI18nResource.Engine.page_title()
    BrowserEnginePageRender(this, modifier)
  }

  override suspend fun destroy() {
  }
}