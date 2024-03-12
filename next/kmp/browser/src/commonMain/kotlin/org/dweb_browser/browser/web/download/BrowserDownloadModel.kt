package org.dweb_browser.browser.web.download

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import io.ktor.http.URLBuilder
import kotlinx.coroutines.launch
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.ext.createChannelOfDownload
import org.dweb_browser.browser.download.ext.createDownloadTask
import org.dweb_browser.browser.download.ext.currentDownload
import org.dweb_browser.browser.download.ext.existsDownload
import org.dweb_browser.browser.download.ext.pauseDownload
import org.dweb_browser.browser.download.ext.startDownload
import org.dweb_browser.browser.web.BrowserNMM
import org.dweb_browser.browser.web.debugBrowser
import org.dweb_browser.dwebview.WebDownloadArgs
import org.dweb_browser.sys.window.core.modal.WindowBottomSheetsController
import org.dweb_browser.sys.window.ext.createBottomSheets

enum class BrowserDownloadManagePage {
  Manage, DeleteAll, MoreCompleted, DeleteCompleted
}

class BrowserDownloadModel(private val browserNMM: BrowserNMM) {
  val store = BrowserDownloadStore(browserNMM)
  val saveDownloadMaps: MutableMap<String, MutableList<BrowserDownloadItem>> = mutableStateMapOf()
  private val newDownloadMaps: HashMap<String, BrowserDownloadItem> = hashMapOf()
  val alreadyExist = mutableStateOf(false)
  val managePage = mutableStateOf(BrowserDownloadManagePage.Manage)

  init {
    browserNMM.ioAsyncScope.launch {
      saveDownloadMaps.putAll(store.getAll())
      saveDownloadMaps.forEach { (key, items) ->
        var save = false
        items.forEach { item ->
          if (item.state.state == DownloadState.Downloading) {
            save = true
            val current = item.taskId?.let { taskId -> browserNMM.currentDownload(taskId) } ?: 0L
            item.state = item.state.copy(current = current, state = DownloadState.Paused)
          }
        }
        if (save) store.save(key, items)
      }
    }
  }

  fun clickDownloadButton(downloadItem: BrowserDownloadItem) = browserNMM.ioAsyncScope.launch {
    when (downloadItem.state.state) {
      DownloadState.Completed -> {
        if (downloadItem.fileSuffix.type == BrowserDownloadType.Application) {
          // TODO 打开安装界面
        } else {
          // TODO 打开文件
        }
      }

      DownloadState.Downloading -> {
        pauseDownload(downloadItem)
      }

      else -> {
        startDownload(downloadItem)
      }
    }
  }

  /**
   * 创建任务，如果存在则恢复
   */
  private suspend fun startDownload(item: BrowserDownloadItem) {
    val exist = item.taskId?.let { browserNMM.existsDownload(it) } ?: false
    if (!exist) {
      item.taskId = browserNMM.createDownloadTask(
        item.downloadArgs.url, item.downloadArgs.contentLength, external = true
      )
      item.alreadyWatch = false
      saveDownloadMaps.getOrPut(item.urlKey) { mutableStateListOf() }.add(item)
    }
    if (!item.alreadyWatch) {
      watchProcess(browserDownloadItem = item)
    }
    browserNMM.startDownload(item.taskId!!)
  }

  private suspend fun pauseDownload(item: BrowserDownloadItem) = item.taskId?.let { taskId ->
    browserNMM.pauseDownload(taskId)
  }

  private suspend fun watchProcess(browserDownloadItem: BrowserDownloadItem) {
    val taskId = browserDownloadItem.taskId ?: return
    browserNMM.ioAsyncScope.launch {
      browserDownloadItem.alreadyWatch = true
      val res = browserNMM.createChannelOfDownload(taskId) {
        val lastState = browserDownloadItem.state.state
        browserDownloadItem.state = browserDownloadItem.state.copy(
          current = downloadTask.status.current,
          total = downloadTask.status.total,
          state = downloadTask.status.state
        )
        if (lastState != downloadTask.status.state) {
          saveDownloadMaps[browserDownloadItem.urlKey]?.let {
            store.save(
              browserDownloadItem.urlKey, it
            )
          }
        }
        when (downloadTask.status.state) {
          DownloadState.Completed -> {
            // 关闭watchProcess
            channel.close()
            browserDownloadItem.alreadyWatch = false
          }

          else -> {}
        }
      }
      debugBrowser("watchProcess", "/watch process error=>$res")
    }
  }

  private var downloadModal: WindowBottomSheetsController? = null
  internal suspend fun openDownloadView(webDownloadArgs: WebDownloadArgs) {
    alreadyExist.value = true
    val urlKey = URLBuilder(webDownloadArgs.url).apply { parameters.clear() }.buildString()
    val item = newDownloadMaps.getOrPut(urlKey) {
      alreadyExist.value = false
      BrowserDownloadItem(urlKey, downloadArgs = webDownloadArgs).apply {
        val name = webDownloadArgs.suggestedFilename


        val suffix = name.split(".").last()
        fileSuffix = FileSuffix.entries.find { it.suffix == suffix } ?: FileSuffix.Other
        // 判断当前的名称是否在历史中已存在，如果已存在就进行名称去重
        fileName = saveDownloadMaps[urlKey]?.let { saveItems ->
          var index = 1
          var tempName: String
          do {
            tempName = name.substringBeforeLast(".") + "_${index}." + fileSuffix.suffix
            if (saveItems.find { saveItem -> saveItem.fileName == tempName } == null) break // 没找到，表示可以使用
            index++
          } while (true)
          tempName
        } ?: name
      }
    }

    downloadModal = browserNMM.createBottomSheets { BrowserDownloadSheet(item) }.apply { open() }
  }

  internal fun close() = browserNMM.ioAsyncScope.launch {
    downloadModal?.close()
    downloadModal = null
  }
}