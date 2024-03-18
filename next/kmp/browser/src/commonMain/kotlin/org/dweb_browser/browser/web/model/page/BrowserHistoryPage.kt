package org.dweb_browser.browser.web.model.page

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.History
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.dweb_browser.browser.web.BrowserController

class BrowserHistoryPage(browserController: BrowserController) : BrowserPage(browserController) {
  companion object {
    fun isHistoryUrl(url: String) = isAboutPage(url, "history")
  }

  override val icon
    @Composable get() = rememberVectorPainter(Icons.TwoTone.History)
  override val iconColorFilter
    @Composable get() = ColorFilter.tint(LocalContentColor.current)

  init {
    url = "about:history"
  }
  override fun isUrlMatch(url: String) = isHistoryUrl(url)

  @Composable
  override fun Render(modifier: Modifier) {

  }

  override suspend fun destroy() {
  }
}