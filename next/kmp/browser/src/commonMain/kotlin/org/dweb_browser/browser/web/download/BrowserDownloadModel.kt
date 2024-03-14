package org.dweb_browser.browser.web.download

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import io.ktor.http.URLBuilder
import kotlinx.coroutines.launch
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.ext.createChannelOfDownload
import org.dweb_browser.browser.download.ext.createDownloadTask
import org.dweb_browser.browser.download.ext.currentDownload
import org.dweb_browser.browser.download.ext.existsDownload
import org.dweb_browser.browser.download.ext.pauseDownload
import org.dweb_browser.browser.download.ext.removeDownload
import org.dweb_browser.browser.download.ext.startDownload
import org.dweb_browser.browser.web.BrowserNMM
import org.dweb_browser.browser.web.debugBrowser
import org.dweb_browser.browser.web.download.view.BrowserDownloadSheet
import org.dweb_browser.dwebview.WebDownloadArgs
import org.dweb_browser.sys.window.core.modal.WindowBottomSheetsController
import org.dweb_browser.sys.window.ext.createBottomSheets

enum class BrowserDownloadManagePage {
  Manage, DeleteAll, MoreCompleted, DeleteCompleted
}

class BrowserDownloadModel(private val browserNMM: BrowserNMM) {
  val store = BrowserDownloadStore(browserNMM)
  val saveDownloadList: MutableList<BrowserDownloadItem> = mutableStateListOf()
  val saveCompleteList: MutableList<BrowserDownloadItem> = mutableStateListOf()
  private val newDownloadMaps: HashMap<String, BrowserDownloadItem> = hashMapOf()
  val alreadyExist = mutableStateOf(false)
  val managePage = mutableStateOf(BrowserDownloadManagePage.Manage)

  init {
    browserNMM.ioAsyncScope.launch {
      saveCompleteList.addAll(store.getCompleteAll())
      saveDownloadList.addAll(store.getDownloadAll())
      var save = false
      saveDownloadList.forEach { item ->
        if (item.state.state == DownloadState.Downloading) {
          save = true
          val current = item.taskId?.let { taskId -> browserNMM.currentDownload(taskId) } ?: 0L
          if (current >= 0L) {
            item.state = item.state.copy(current = current, state = DownloadState.Paused)
          } else { // 如果是 -1L 表示在下载列表中没有找到该记录，直接初始化
            item.state = item.state.copy(current = 0L, state = DownloadState.Init)
          }
        }

      }
      if (save) saveListToStore()
    }
  }

  /**
   * 用于响应点击“下载中”列表的按钮
   */
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
   * 用于响应点击“已下载”列表的按钮
   */
  fun clickCompleteButton(downloadItem: BrowserDownloadItem) = browserNMM.ioAsyncScope.launch {
    // TODO 比如打开文档，打开应用安装界面等
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

      // 如果重新下载时，需要将 已完成 和 下载中 列表的数据删除，然后将该记录插入到 下载中 的列表
      saveCompleteList.remove(item)
      saveDownloadList.remove(item)
      saveDownloadList.add(0, item)
      saveListToStore(complete = true)
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
          // 如果是完成的话，需要添加到 “已下载”列表并保存，如果是其他状态，直接保存“下载中”列表
          if (downloadTask.status.state == DownloadState.Completed) {
            saveDownloadList.remove(browserDownloadItem)
            saveCompleteList.add(0, browserDownloadItem)
            saveListToStore(complete = true)
          } else {
            saveListToStore()
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
    downloadModal = browserNMM.createBottomSheets { BrowserDownloadSheet(item) }.apply { open() }

    // 如果当前 item 的状态不是 DownloadState.Init 的话，直接删除，重建下载
    if (item.state.state != DownloadState.Init) {
      item.taskId?.let { taskId ->
        browserNMM.removeDownload(taskId)
        item.taskId = null
      }
      item.state = item.state.copy(state = DownloadState.Init, current = 0L)
      clickDownloadButton(item)
    }
  }

  private fun saveListToStore(download: Boolean = true, complete: Boolean = false) =
    browserNMM.ioAsyncScope.launch {
      if (download) store.saveDownloadList(saveDownloadList)
      if (complete) store.saveCompleteList(saveCompleteList)
    }

  internal fun close() = browserNMM.ioAsyncScope.launch {
    downloadModal?.close()
    downloadModal = null
  }

  fun deleteDownloadItems(list: MutableList<BrowserDownloadItem>) = browserNMM.ioAsyncScope.launch {
    list.forEach { item -> item.taskId?.let { taskId -> browserNMM.removeDownload(taskId) } }
    saveCompleteList.removeAll(list)
    saveDownloadList.removeAll(list)
    saveListToStore(download = true, complete = true)
  }
}