package org.dweb_browser.browser.web.model.page

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.BrowserController

class BrowserSettingPage(browserController: BrowserController) : BrowserPage(browserController) {
  companion object {
    fun isSettingUrl(url: String) = isAboutPage(url, "settings")
  }

  override val icon
    @Composable get() = rememberVectorPainter(Icons.TwoTone.Settings)
  override val iconColorFilter
    @Composable get() = ColorFilter.tint(LocalContentColor.current)

  init {
    url = "about:downloads"
  }

  override fun isUrlMatch(url: String) = isSettingUrl(url)

  @Composable
  override fun Render(modifier: Modifier) {
    title = BrowserI18nResource.Setting.page_title()
  }

  override suspend fun destroy() {
  }
}