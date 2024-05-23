package org.dweb_browser.browser.web.model.page

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.data.WebSiteInfo
import org.dweb_browser.browser.web.ui.page.BrowserHistoryPageRender

class BrowserHistoryPage(browserController: BrowserController) : BrowserPage(browserController) {
  companion object {
    fun isHistoryUrl(url: String) = BrowserPageType.History.isMatchUrl(url) // isAboutPage(url, "history")
  }

  override val icon
    @Composable get() = BrowserPageType.History.iconPainter() // rememberVectorPainter(Icons.TwoTone.History)
  override val iconColorFilter
    @Composable get() = ColorFilter.tint(LocalContentColor.current)

  init {
    url = BrowserPageType.History.url // "about:history"
  }

  override fun isUrlMatch(url: String) = isHistoryUrl(url)

  var isInEditMode by mutableStateOf(false)
  val selectedHistories = mutableStateListOf<WebSiteInfo>()

  @Composable
  override fun Render(modifier: Modifier) {
    title = BrowserI18nResource.History.page_title()
    BrowserHistoryPageRender(this, modifier)
  }

  override suspend fun destroy() {
  }
}