package org.dweb_browser.browser.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import org.dweb_browser.browser.web.data.BrowserDownloadItem
import org.dweb_browser.browser.web.data.BrowserDownloadStore
import org.dweb_browser.browser.web.data.BrowserDownloadType
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.std.file.ext.realFile
import org.dweb_browser.dwebview.WebDownloadArgs
import org.dweb_browser.helper.PromiseOut

class BrowserDownloadController(
  private val browserNMM: BrowserNMM, private val browserController: BrowserController
) {
  private val downloadStore = BrowserDownloadStore(browserNMM)

  val saveDownloadList: MutableList<BrowserDownloadItem> = mutableStateListOf()
  val saveCompleteList: MutableList<BrowserDownloadItem> = mutableStateListOf()
  private val newDownloadMaps: HashMap<String, BrowserDownloadItem> = hashMapOf() // 保存当前启动后新增的临时列表
  var curDownloadItem by mutableStateOf<BrowserDownloadItem?>(null)
  var alreadyExists by mutableStateOf(false) // 用于判断当前的下载地址是否在 newDownloadMaps 中

  init {
    // 初始化下载数据
    browserNMM.ioAsyncScope.launch {
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
  private fun saveDownloadList(download: Boolean = true, complete: Boolean = false) =
    browserNMM.ioAsyncScope.launch {
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
            browserDownloadItem.filePath = browserNMM.realFile(downloadTask.filepath) // 保存下载路径
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

  fun deleteDownloadItems(list: List<BrowserDownloadItem>) = browserNMM.ioAsyncScope.launch {
    list.forEach { item -> item.taskId?.let { taskId -> browserNMM.removeDownload(taskId) } }
    saveCompleteList.removeAll(list)
    saveDownloadList.removeAll(list)
    saveDownloadList(download = true, complete = true)
  }

  /**
   * 打开网页下载的提示框
   */
  suspend fun openDownloadDialog(webDownloadArgs: WebDownloadArgs) {
    val urlKey = URLBuilder(webDownloadArgs.url).apply { parameters.clear() }.buildString()
    alreadyExists = true // 获取状态前，先置为 true
    curDownloadItem = newDownloadMaps.getOrPut(urlKey) {
      alreadyExists = false // 如果是属于新增的，那么就是不存在的，状态为 false
      BrowserDownloadItem(urlKey, downloadArgs = webDownloadArgs).apply {
        val name = webDownloadArgs.suggestedFilename
        val suffix = name.split(".").last()
        fileType = BrowserDownloadType.entries.find { downloadType ->
          downloadType.matchSuffix(suffix)
        } ?: BrowserDownloadType.Other

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
          tmpName = name.substringBeforeLast(".") + "_${index}." + suffix
          index++
        } while (true)
      }
    }
  }

  /**
   * 隐藏网页下载的提示框
   */
  fun closeDownloadDialog() {
    curDownloadItem = null
    alreadyExists = false
  }

  /**
   * 用于响应点击“下载中”列表的按钮
   */
  fun clickDownloadButton(downloadItem: BrowserDownloadItem) = browserNMM.ioAsyncScope.launch {
    when (downloadItem.state.state) {
      DownloadState.Completed -> {
        openFileOnDownload(downloadItem) // 直接调用系统级别的打开文件操作
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
   * 用于响应重新下载操作，主要就是网页点击下载后，如果判断列表中已经存在下载数据时调用
   */
  fun clickRetryButton(downloadItem: BrowserDownloadItem) = browserNMM.ioAsyncScope.launch {
    // 将状态进行修改下，然后启动下载
    alreadyExists = false
    if (downloadItem.state.state != DownloadState.Init) {
      downloadItem.taskId?.let { taskId ->
        browserNMM.removeDownload(taskId)
        downloadItem.taskId = null
      }
      downloadItem.state = downloadItem.state.copy(state = DownloadState.Init, current = 0L)
      clickDownloadButton(downloadItem)
    }
  }

  suspend fun shareDownloadItem(downloadItem: BrowserDownloadItem): Boolean {
    val ipc = browserNMM.connect("share.sys.dweb")
    ipc.postMessage(
      IpcEvent.fromUtf8("shareLocalFile", downloadItem.filePath)
    )
    val sharePromiseOut = PromiseOut<String>()
    ipc.onEvent { (event, ipc) ->
      if (event.name == "shareLocalFile") {
        sharePromiseOut.resolve(event.data as String)
        ipc.close()
      }
    }
    return sharePromiseOut.waitPromise() == "success"
  }

  suspend fun openFileOnDownload(downloadItem: BrowserDownloadItem) =
    openFileByPath(realPath = downloadItem.filePath, justInstall = false)
}
