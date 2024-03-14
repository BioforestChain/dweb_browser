package org.dweb_browser.browser.web.data.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.data.WebSiteInfo
import org.dweb_browser.browser.web.ui.page.BrowserBookmarkPageRender

class BrowserBookmarkPage(browserController: BrowserController) : BrowserPage(browserController) {
  companion object {
    fun isBookmarkUrl(url: String) = isAboutPage(url, "bookmarks")
  }

  var isInEditMode by mutableStateOf(false)
  var editingBookmark by mutableStateOf<WebSiteInfo?>(null)
  val selectedBookmarks = mutableStateListOf<WebSiteInfo>()

  override fun isUrlMatch(url: String) = isBookmarkUrl(url)

  @Composable
  override fun Render(modifier: Modifier) {
    BrowserBookmarkPageRender(this, modifier)
  }

  override suspend fun destroy() {
  }
}