package org.dweb_browser.browser.web.model.page

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Bookmarks
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.data.WebSiteInfo
import org.dweb_browser.browser.web.ui.page.BrowserBookmarkPageRender

class BrowserBookmarkPage(browserController: BrowserController) : BrowserPage(browserController) {
  companion object {
    fun isBookmarkUrl(url: String) = isAboutPage(url, "bookmarks")
  }

  override val icon
    @Composable get() = rememberVectorPainter(Icons.TwoTone.Bookmarks)
  override val iconColorFilter
    @Composable get() = ColorFilter.tint(LocalContentColor.current)

  init {
    url = "about:bookmarks"
  }

  var isInEditMode by mutableStateOf(false)
  var editingBookmark by mutableStateOf<WebSiteInfo?>(null)
  val selectedBookmarks = mutableStateListOf<WebSiteInfo>()

  override fun isUrlMatch(url: String) = isBookmarkUrl(url)

  @Composable
  override fun Render(modifier: Modifier) {
    title = BrowserI18nResource.browser_bookmark_page_title()
    BrowserBookmarkPageRender(this, modifier)
  }

  override suspend fun destroy() {
  }
}