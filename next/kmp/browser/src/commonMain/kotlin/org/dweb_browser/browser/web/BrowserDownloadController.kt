package org.dweb_browser.browser.web

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.launch
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.ext.createChannelOfDownload
import org.dweb_browser.browser.download.ext.createDownloadTask
import org.dweb_browser.browser.download.ext.currentDownload
import org.dweb_browser.browser.download.ext.existsDownload
import org.dweb_browser.browser.download.ext.pauseDownload
import org.dweb_browser.browser.download.ext.removeDownload
import org.dweb_browser.browser.download.ext.startDownload
import org.dweb_browser.browser.web.data.BrowserDownloadItem
import org.dweb_browser.browser.web.data.BrowserDownloadStore
import org.dweb_browser.browser.web.data.BrowserDownloadType
import org.dweb_browser.browser.web.model.BrowserDownloadModel
import org.dweb_browser.dwebview.WebDownloadArgs

class BrowserDownloadController(
  private val browserNMM: BrowserNMM, private val browserController: BrowserController
) {
  private val downloadStore = BrowserDownloadStore(browserNMM)

  val saveDownloadList: MutableList<BrowserDownloadItem> = mutableStateListOf()
  val saveCompleteList: MutableList<BrowserDownloadItem> = mutableStateListOf()
  private val downloadModel = BrowserDownloadModel(this, browserNMM)

  init {
    // 初始化下载数据
    browserNMM.mmScope.launch {
      saveCompleteList.addAll(downloadStore.getCompleteAll())
      saveDownloadList.addAll(downloadStore.getDownloadAll())
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
      if (save) saveDownloadList() // 只保存下载中的内容
    }
  }

  /**
   * 保存下载的数据
   */
  fun saveDownloadList(download: Boolean = true, complete: Boolean = false) =
    browserNMM.mmScope.launch {
      if (download) downloadStore.saveDownloadList(saveDownloadList)
      if (complete) downloadStore.saveCompleteList(saveCompleteList)
    }

  /**
   * 创建任务，如果存在则恢复
   */
  suspend fun startDownload(item: BrowserDownloadItem) {
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
      saveDownloadList(complete = true)
    }
    if (!item.alreadyWatch) {
      watchProcess(browserDownloadItem = item)
    }
    browserNMM.startDownload(item.taskId!!)
  }

  suspend fun pauseDownload(item: BrowserDownloadItem) = item.taskId?.let { taskId ->
    browserNMM.pauseDownload(taskId)
  }

  private suspend fun watchProcess(browserDownloadItem: BrowserDownloadItem) {
    val taskId = browserDownloadItem.taskId ?: return
    browserNMM.mmScope.launch {
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
            saveDownloadList(complete = true)
          } else {
            saveDownloadList()
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

  fun deleteDownloadItems(list: MutableList<BrowserDownloadItem>) = browserNMM.mmScope.launch {
    list.forEach { item -> item.taskId?.let { taskId -> browserNMM.removeDownload(taskId) } }
    saveCompleteList.removeAll(list)
    saveDownloadList.removeAll(list)
    saveDownloadList(download = true, complete = true)
  }

  suspend fun openDownloadView(args: WebDownloadArgs) = downloadModel.openDownloadView(args)

  /**
   * 用于响应点击“下载中”列表的按钮
   */
  fun clickDownloadButton(downloadItem: BrowserDownloadItem) = browserNMM.mmScope.launch {
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
  fun clickCompleteButton(downloadItem: BrowserDownloadItem) = browserNMM.mmScope.launch {
    // TODO 比如打开文档，打开应用安装界面等
  }
}