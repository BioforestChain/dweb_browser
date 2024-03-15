package org.dweb_browser.browser.web.data.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.data.BrowserDownloadItem
import org.dweb_browser.browser.web.ui.page.BrowserDownloadPageRender

enum class DownloadPage {
  Manage, DeleteAll, MoreCompleted, DeleteCompleted
}

class BrowserDownloadPage(browserController: BrowserController) : BrowserPage(browserController) {
  companion object {
    fun isDownloadUrl(url: String) = isAboutPage(url, "downloads")
  }

  var downloadPage by mutableStateOf(DownloadPage.Manage)
  private val downloadController = browserController.downloadController
  val saveDownloadList = downloadController.saveDownloadList
  val saveCompleteList = downloadController.saveCompleteList

  override fun isUrlMatch(url: String) = isDownloadUrl(url)

  @Composable
  override fun Render(modifier: Modifier) {
    BrowserDownloadPageRender(modifier)
  }

  override suspend fun destroy() {
  }

  /**
   * 用于响应点击“下载中”列表的按钮
   */
  fun clickDownloadButton(downloadItem: BrowserDownloadItem) =
    downloadController.clickDownloadButton(downloadItem)

  /**
   * 用于响应点击“已下载”列表的按钮
   */
  fun clickCompleteButton(downloadItem: BrowserDownloadItem) =
    downloadController.clickCompleteButton(downloadItem)

  fun deleteDownloadItems(list: MutableList<BrowserDownloadItem>) =
    downloadController.deleteDownloadItems(list)
}