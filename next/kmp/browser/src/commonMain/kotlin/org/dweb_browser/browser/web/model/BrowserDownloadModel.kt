package org.dweb_browser.browser.web.model

import androidx.compose.runtime.mutableStateOf
import io.ktor.http.URLBuilder
import kotlinx.coroutines.launch
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.ext.removeDownload
import org.dweb_browser.browser.web.BrowserDownloadController
import org.dweb_browser.browser.web.BrowserNMM
import org.dweb_browser.browser.web.data.BrowserDownloadItem
import org.dweb_browser.browser.web.data.FileSuffix
import org.dweb_browser.browser.web.ui.BrowserDownloadSheet
import org.dweb_browser.dwebview.WebDownloadArgs
import org.dweb_browser.sys.window.core.modal.WindowBottomSheetsController
import org.dweb_browser.sys.window.ext.createBottomSheets

class BrowserDownloadModel(
  private val downloadController: BrowserDownloadController,
  private val browserNMM: BrowserNMM,
) {
  val saveDownloadList = downloadController.saveDownloadList
  val saveCompleteList = downloadController.saveCompleteList
  private val newDownloadMaps: HashMap<String, BrowserDownloadItem> = hashMapOf()
  val alreadyExist = mutableStateOf(false)

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

  /**
   * 用于响应重新下载操作，主要就是网页点击下载后，如果判断列表中已经存在下载数据时调用
   */
  fun clickRetryButton(downloadItem: BrowserDownloadItem) = browserNMM.mmScope.launch {
    // 将状态进行修改下，然后启动下载

    if (downloadItem.state.state != DownloadState.Init) {
      downloadItem.taskId?.let { taskId ->
        browserNMM.removeDownload(taskId)
        downloadItem.taskId = null
      }
      downloadItem.state = downloadItem.state.copy(state = DownloadState.Init, current = 0L)
      clickDownloadButton(downloadItem)
    }

    alreadyExist.value = false
  }

  private var downloadSheet: WindowBottomSheetsController? = null
  internal suspend fun openDownloadView(webDownloadArgs: WebDownloadArgs) {
    alreadyExist.value = true
    val urlKey = URLBuilder(webDownloadArgs.url).apply { parameters.clear() }.buildString()
    val item = newDownloadMaps.getOrPut(urlKey) {
      alreadyExist.value = false
      BrowserDownloadItem(urlKey, downloadArgs = webDownloadArgs).apply {
        val name = webDownloadArgs.suggestedFilename

        val suffix = name.split(".").last()
        fileSuffix = FileSuffix.entries.find { it.suffix == suffix } ?: FileSuffix.Other

        // 名称去重操作
        var index = 1
        var tmpName: String = name
        do {
          if (
            saveDownloadList.firstOrNull { it.fileName == tmpName && it.urlKey == urlKey } == null &&
            saveCompleteList.firstOrNull { it.fileName == tmpName && it.urlKey == urlKey } == null
          ) {
            fileName = tmpName
            break
          }
          tmpName = name.substringBeforeLast(".") + "_${index}." + fileSuffix.suffix
          index++
        } while (true)
      }
    }
    downloadSheet = browserNMM.createBottomSheets { BrowserDownloadSheet(item) }.apply { open() }
  }

  fun close() = browserNMM.mmScope.launch {
    downloadSheet?.close()
    downloadSheet = null
  }
}