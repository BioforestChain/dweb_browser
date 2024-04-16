package org.dweb_browser.browser.web.model.page

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Download
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.data.BrowserDownloadItem
import org.dweb_browser.browser.web.ui.page.BrowserDownloadPageRender

class BrowserDownloadPage(
  private val browserController: BrowserController
) : BrowserPage(browserController) {
  companion object {
    fun isDownloadUrl(url: String) = isAboutPage(url, "downloads")
  }

  override val icon
    @Composable get() = rememberVectorPainter(Icons.TwoTone.Download)
  override val iconColorFilter
    @Composable get() = ColorFilter.tint(LocalContentColor.current)

  init {
    url = "about:downloads"
  }

  private val downloadController = browserController.downloadController
  val saveDownloadList = downloadController.saveDownloadList
  val saveCompleteList = downloadController.saveCompleteList

  override fun isUrlMatch(url: String) = isDownloadUrl(url)

  @Composable
  override fun Render(modifier: Modifier) {
    title = BrowserI18nResource.Download.page_title()
    BrowserDownloadPageRender(modifier)
  }

  override suspend fun destroy() {
  }

  /**
   * 用于响应点击列表的按钮
   */
  fun clickDownloadButton(downloadItem: BrowserDownloadItem) =
    downloadController.clickDownloadButton(downloadItem)

  fun shareDownloadItem(downloadItem: BrowserDownloadItem) = browserController.ioScope.launch {
    downloadController.shareDownloadItem(downloadItem)
  }

  fun deleteDownloadItems(list: List<BrowserDownloadItem>) =
    downloadController.deleteDownloadItems(list)

  fun openFileOnDownload(downloadItem: BrowserDownloadItem) = browserController.ioScope.launch {
    downloadController.openFileOnDownload(downloadItem)
  }
}