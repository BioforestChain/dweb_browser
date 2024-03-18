package org.dweb_browser.browser.web.model.page

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.History
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
import org.dweb_browser.browser.web.ui.page.BrowserHistoryPageRender

class BrowserHistoryPage(
  private val browserController: BrowserController
) : BrowserPage(browserController) {
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

  var isInEditMode by mutableStateOf(false)
  val selectedHistories = mutableStateListOf<WebSiteInfo>()
  val historyMap get() = browserController.historys.value

  @Composable
  override fun Render(modifier: Modifier) {
    BrowserHistoryPageRender(this, modifier)
  }

  override suspend fun destroy() {
  }

  suspend fun tryOpenUrlUI(url: String) = browserController.viewModel.tryOpenUrlUI(url)

  suspend fun removeHistoryLink() {
    if (selectedHistories.isEmpty()) return
    selectedHistories.forEach { item ->
      historyMap[item.day.toString()]?.remove(item)
    }
    // TODO 存储修改后的数据
  }
}