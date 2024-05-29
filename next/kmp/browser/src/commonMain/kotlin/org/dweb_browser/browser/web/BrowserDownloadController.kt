package org.dweb_browser.browser.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.http.URLBuilder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.DownloadStateEvent
import org.dweb_browser.browser.download.ext.createDownloadTask
import org.dweb_browser.browser.download.ext.downloadProgressFlow
import org.dweb_browser.browser.download.ext.existDownloadTask
import org.dweb_browser.browser.download.ext.getDownloadTask
import org.dweb_browser.browser.download.ext.pauseDownload
import org.dweb_browser.browser.download.ext.removeDownload
import org.dweb_browser.browser.download.ext.startDownload
import org.dweb_browser.browser.web.data.BrowserDownloadItem
import org.dweb_browser.browser.web.data.BrowserDownloadStore
import org.dweb_browser.browser.web.data.BrowserDownloadType
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.dwebview.WebDownloadArgs
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.valueIn

class BrowserDownloadController(
  private val browserNMM: BrowserNMM.BrowserRuntime,
  private val browserController: BrowserController,
) {
  private val downloadStore = BrowserDownloadStore(browserNMM)

  val downloadList: MutableList<BrowserDownloadItem> = mutableStateListOf()
  val completeList: MutableList<BrowserDownloadItem> = mutableStateListOf()
  private val newDownloadMaps: HashMap<String, BrowserDownloadItem> = hashMapOf() // 保存当前启动后新增的临时列表
  var curDownloadItem by mutableStateOf<BrowserDownloadItem?>(null)
  var alreadyExists by mutableStateOf(false) // 用于判断当前的下载地址是否在 newDownloadMaps 中

  init {
    // 初始化下载数据
    browserNMM.scopeLaunch(cancelable = true) {
      completeList.addAll(downloadStore.getCompleteAll())
      downloadList.addAll(downloadStore.getDownloadAll())
      var save = false
      downloadList.forEach { item ->
        if (item.state.state.valueIn(DownloadState.Downloading, DownloadState.Paused)) {
          save = true
          item.state = item.taskId?.let { browserNMM.getDownloadTask(it)?.status }?.let { status ->
            if (status.state != DownloadState.Completed && status.current >= 0L) {
              item.state.copy(current = status.current, total = status.total, state = status.state)
            } else null // 如果下载状态已完成了，但是当前记录是下载中，目前考虑直接移除重下，TODO 另一中处理方案就是直接打开安装界面？？
          } ?: run {
            item.taskId?.let { taskId -> browserNMM.removeDownload(taskId) }
            item.state.copy(current = 0L, state = DownloadState.Init)
          }
        }
      }
      if (save) saveDownloadList() // 只保存下载中的内容
    }
  }

  /**
   * 保存下载的数据
   */
  private fun saveDownloadList(download: Boolean = true, complete: Boolean = false) {
    browserNMM.scopeLaunch(cancelable = false) {
      downloadStore.saveDownloadList(downloadList)
    }
  }

  private fun saveCompleteList() {
    browserNMM.scopeLaunch(cancelable = false) {
      downloadStore.saveCompleteList(completeList)
    }
  }

  private val downloadLock = Mutex()

  /**
   * 创建任务，如果存在则恢复
   */
  suspend fun startDownload(item: BrowserDownloadItem) = downloadLock.withLock {
    var taskId = item.taskId
    if (taskId == null || browserNMM.existDownloadTask(taskId)) {
      val downloadTask = browserNMM.createDownloadTask(
        item.downloadArgs.url, item.downloadArgs.contentLength, external = true
      )
      item.taskId = downloadTask.id
      taskId = downloadTask.id

      /// 如果重新下载时，需要将 已完成 和 下载中 列表的数据删除，然后将该记录插入到 下载中 的列表
      completeList.remove(item)
      saveCompleteList()

      downloadList.remove(item)
      downloadList.add(0, item)
      saveCompleteList()

      /// 监听
      browserNMM.scopeLaunch(cancelable = true) {
        if (watchProcess(taskId, item)) {
          // 如果是完成的话，需要添加到 “已下载”列表并保存，如果是其他状态，直接保存“下载中”列表
          if (downloadTask.status.state == DownloadState.Completed) {
            downloadList.remove(item)
            saveDownloadList()

            completeList.add(0, item)
            item.filePath = downloadTask.filepath // 保存下载路径
            saveCompleteList()
          } else {
            saveDownloadList()
          }
        }
      }
    }

    browserNMM.startDownload(item.taskId!!)
  }

  suspend fun pauseDownload(item: BrowserDownloadItem) = item.taskId?.let { taskId ->
    browserNMM.pauseDownload(taskId)
  }


  private suspend fun watchProcess(
    taskId: String,
    browserDownloadItem: BrowserDownloadItem,
  ): Boolean {
    var success = false;
    browserNMM.downloadProgressFlow(taskId).collect { status ->
      if (status.state == DownloadState.Completed) {
        success = true
      }
      val newStatus = DownloadStateEvent(
        current = status.current, total = status.total, state = status.state
      )
      if (newStatus != browserDownloadItem.state) {
        browserDownloadItem.state = newStatus
        saveDownloadList()
      }
    }
    return success
  }

  fun deleteDownloadItems(list: List<BrowserDownloadItem>) =
    browserNMM.scopeLaunch(cancelable = true) {
      list.forEach { item -> item.taskId?.let { taskId -> browserNMM.removeDownload(taskId) } }
      completeList.removeAll(list)
      saveCompleteList()

      downloadList.removeAll(list)
      saveDownloadList()
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
          if (downloadList.firstOrNull { it.fileName == tmpName && it.urlKey == urlKey } == null && completeList.firstOrNull { it.fileName == tmpName && it.urlKey == urlKey } == null) {
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
  fun clickDownloadButton(downloadItem: BrowserDownloadItem) =
    browserNMM.scopeLaunch(cancelable = true) {
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
  fun clickRetryButton(downloadItem: BrowserDownloadItem) =
    browserNMM.scopeLaunch(cancelable = true) {
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

  /// TODO waterbang fix this
  suspend fun shareDownloadItem(downloadItem: BrowserDownloadItem): Boolean {
    val ipc = browserNMM.connect("share.sys.dweb")
    ipc.postMessage(
      IpcEvent.fromUtf8("shareLocalFile", downloadItem.filePath)
    )
    val sharePromiseOut = PromiseOut<String>()
    ipc.onEvent("shareLocalFile").collectIn(browserNMM.getRuntimeScope()) { event ->
      event.consumeFilter { ipcEvent ->
        (ipcEvent.name == "shareLocalFile").trueAlso {
          sharePromiseOut.resolve(ipcEvent.data as String)
          ipc.close()
        }
      }
    }
    return sharePromiseOut.waitPromise() == "success"
  }

  suspend fun openFileOnDownload(downloadItem: BrowserDownloadItem) =
    openFileByPath(realPath = downloadItem.filePath, justInstall = false)
}
