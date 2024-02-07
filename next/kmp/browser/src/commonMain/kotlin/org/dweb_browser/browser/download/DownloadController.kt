package org.dweb_browser.browser.download

import androidx.compose.runtime.mutableStateListOf
import io.ktor.http.ContentRange
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.RangeUnits
import io.ktor.http.fromFilePath
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.close
import io.ktor.utils.io.core.ByteReadPacket
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.download.model.ChangeableMutableMap
import org.dweb_browser.browser.download.model.ChangeableType
import org.dweb_browser.browser.download.model.DownloadModel
import org.dweb_browser.browser.download.ui.DecompressModel
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.FileMetadata
import org.dweb_browser.core.std.file.ext.appendFile
import org.dweb_browser.core.std.file.ext.existFile
import org.dweb_browser.core.std.file.ext.infoFile
import org.dweb_browser.core.std.file.ext.removeFile
import org.dweb_browser.helper.Queue
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.consumeEachArrayRange
import org.dweb_browser.helper.createByteChannel
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureStreamBody
import org.dweb_browser.sys.toast.ext.showToast
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.getWindow

@Serializable
data class DownloadTask(
  /** 下载编号 */
  val id: String,
  /** 下载链接 */
  val url: String,
  /** 创建时间 */
  val createTime: Long = datetimeNow(),
  /** 来源模块 */
  val originMmid: MMID,
  /** 来源链接 */
  val originUrl: String?,
  /** 打开应用的跳转地址 */
  val openDappUri: String?,
  /** 文件的元数据类型，可以用来做“打开文件”时的参考类型 */
  var mime: String,
  /** 文件路径 */
  var filepath: String,
  /** 标记当前下载状态 */
  val status: DownloadStateEvent,
/// DBEUG
  var frame: Int = 0,
) {

  @Transient
  var readChannel: ByteReadChannel? = null

  // 监听下载进度 不存储到内存
  @Transient
  private val changeSignal: Signal<DownloadTask> = Signal()

  @Transient
  val emitChanged = Queue.merge {
    changeSignal.emit(this)
    frame++
  }

  @Transient
  val onChange = changeSignal.toListener()

  //  // 帮助实现下载暂停
//  @Transient
//  var paused = PromiseOut<Unit>()
//
  @Transient
  var pauseFlag = false

  @Transient
  var paused = Mutex()

  fun cancel() {
    status.state = DownloadState.Canceled
    status.current = 0L
    readChannel?.cancel()
    readChannel = null
  }

}

@Serializable
enum class DownloadState {
  /** 初始化中，做下载前的准备，包括寻址、创建文件、保存任务等工作 */
  Init,

  /** 下载中*/
  Downloading,

  /** 暂停下载*/
  Paused,

  /** 取消下载*/
  Canceled,

  /** 下载失败*/
  Failed,

  /** 下载完成*/
  Completed,
}

@Serializable
data class DownloadStateEvent(
  var current: Long = 0,
  var total: Long = 1,
  var state: DownloadState = DownloadState.Init,
  var stateMessage: String = ""
) {
}

class DownloadController(private val downloadNMM: DownloadNMM) {
  private val downloadStore = DownloadStore(downloadNMM)
  val downloadTaskMaps: ChangeableMutableMap<TaskId, DownloadTask> =
    ChangeableMutableMap() // 用于监听下载列表
  val downloadList: MutableList<DownloadTask> = mutableStateListOf()
  private var winLock = Mutex(false)
  val downloadModel = DownloadModel(this)
  val decompressModel = DecompressModel(this)

  init {
    // 从内存中恢复状态
    downloadNMM.ioAsyncScope.launch {
      // 状态改变的时候存储保存到内存
      downloadTaskMaps.onChange { (type, _, value) ->
        when (type) {
          ChangeableType.Add -> {
            downloadStore.set(value!!.id, value)
            downloadList.add(0, value)
          }

          ChangeableType.Remove -> {
            downloadStore.delete(value!!.id)
            downloadList.remove(value)
          }

          ChangeableType.PutAll -> {
            downloadList.addAll(
              downloadTaskMaps.toMutableList().sortedByDescending { it.createTime }
            )
          }

          ChangeableType.Clear -> {
            downloadList.clear()
          }
        }
      }
    }
  }

  suspend fun loadDownloadList() {
    downloadTaskMaps.putAll(downloadStore.getAll())
    // 如果是从文件中读取的，需要将下载中的状态统一置为暂停。其他状态保持不变
    downloadTaskMaps.suspendForEach { _, downloadTask ->
      if (downloadTask.status.state == DownloadState.Downloading) {
        if (fileExists(downloadTask.filepath)) { // 为了保证下载中的状态current值正确
          downloadTask.status.current = fileInfo(downloadTask.filepath).size ?: 0L
        }
        downloadTask.status.state = DownloadState.Paused
      }
      debugDownload("LoadList", downloadTask)
    }
  }

  /**
   * 创建新下载任务
   */
  suspend fun createTaskFactory(
    params: DownloadNMM.DownloadTaskParams, originMmid: MMID, externalDownload: Boolean
  ): DownloadTask {
    // 查看是否创建过相同的task,并且相同的task已经下载完成
    val task = DownloadTask(
      id = randomUUID(),
      url = params.url,
      originMmid = originMmid,
      originUrl = params.originUrl,
      openDappUri = params.openDappUri,
      mime = "application/octet-stream",
      filepath = fileCreateByPath(params.url, externalDownload),
      status = DownloadStateEvent(total = params.total)
    )
    downloadTaskMaps.put(task.id, task)
    downloadStore.set(task.id, task) // 保存下载状态
    debugDownload("createTaskFactory", "${task.id} -> $task")
    return task
  }

  /**
   * 恢复(创建)下载，需要重新创建连接🔗
   */
  private suspend fun doDownload(task: DownloadTask): Boolean {
    if (task.readChannel != null) {
      return true
    }
    val start = task.status.current
    debugDownload("recoverDownload", "start=$start => $task")
    task.status.state = DownloadState.Downloading // 这边开始请求http了，属于开始下载
    task.emitChanged()
    val response = downloadNMM.nativeFetch(PureClientRequest(
      href = task.url,
      method = PureMethod.GET,
      headers = PureHeaders().apply {
        init(HttpHeaders.Range, "${RangeUnits.Bytes}=${ContentRange.TailFrom(start)}")
      }
    ))

    if (!response.isOk) {
      task.status.state = DownloadState.Failed
      task.status.stateMessage = response.status.description
      task.emitChanged()
      downloadNMM.showToast(response.status.toString())
      return false
    }

    task.mime = mimeFactory(response.headers, task.url)
    // 判断地址是否支持断点
    val supportRange =
      response.headers.getByIgnoreCase("Accept-Ranges")?.equals("bytes", true) == true
    val contentLength =
      response.headers.getByIgnoreCase("Content-Length")?.toLong() ?: task.status.total

    debugDownload("recoverDownload", "supportRange=$supportRange, contentLength=$contentLength")
    if (supportRange) {
      task.status.current = start
      task.status.total = contentLength + start
    } else {
      task.status.current = 0L
      task.status.total = contentLength
    }
    val streamReader = response.stream().getReader("downloadTask#${task.id}")
    task.readChannel = streamReader
    task.emitChanged()

    debugDownload("downloadFactory", task.id)
    val output = createByteChannel()
    val taskId = task.id
    // 重要记录点 存储到硬盘
    downloadTaskMaps.put(taskId, task)
    // 正式下载需要另外起一个协程，不影响当前的返回值
    downloadNMM.ioAsyncScope.launch {
      debugDownload("middleware", "start id:$taskId current:${task.status.current}")
      task.emitChanged()
      try {
        streamReader.consumeEachArrayRange { byteArray, last ->
          // 处理是否暂停
          task.paused.withLock {}
          if (byteArray.isNotEmpty()) {
            task.status.current += byteArray.size
            output.writePacket(ByteReadPacket(byteArray))
          }
          if (last) {
            output.close()
            streamReader.cancel()
            task.status.state = DownloadState.Completed
            // 触发完成 存储到硬盘
            downloadStore.set(task.id, task)
          } else if (output.isClosedForRead) {
            breakLoop()
            task.cancel()
            // 触发取消 存储到硬盘
            streamReader.cancel()
            downloadStore.set(task.id, task)
          }
          // 触发更新
          task.emitChanged()
          // debugDownload("middleware", "progress id:$taskId current:${downloadTask.status.current}")
        }
        debugDownload("middleware", "end id:$taskId, ${task.status}")
      } catch (e: Throwable) {
        // 这里捕获的一般是 connection reset by peer 当前没有重试机制，用户再次点击即为重新下载
        debugDownload("middleware", "${e.message}")
        task.readChannel?.cancel()
        task.readChannel = null
        task.status.state = DownloadState.Failed
        // 触发失败
        task.emitChanged()
      }
    }
    fileAppend(task, output)
    return true
  }

  private fun mimeFactory(header: PureHeaders, filePath: String): String {
    // 先从header判断
    val contentType = header.get("Content-Type")
    if (!contentType.isNullOrEmpty()) {
      return contentType
    }
    //再从文件判断
    val extension = ContentType.fromFilePath(filePath)
    if (extension.isNotEmpty()) {
      return extension.first().toString()
    }
    return "application/octet-stream"
  }

  /**
   * 创建不重复的文件
   */
  private suspend fun fileCreateByPath(url: String, externalDownload: Boolean): String {
    var index = 0
    val fileName = url.substring(url.lastIndexOf("/") + 1)
    while (true) {
      val path = if (externalDownload) {
        "/download/${index++}_${fileName}"
      } else {
        "/data/download/${index++}_${fileName}"
      }
      if (!fileExists(path)) {
        return path
      }
    }
  }

  private suspend fun fileExists(path: String) = downloadNMM.existFile(path)

  private suspend fun fileInfo(path: String): FileMetadata {
    return Json.decodeFromString(downloadNMM.infoFile(path))
  }

  private suspend fun fileRemove(filepath: String) = downloadNMM.removeFile(filepath)

  //  追加写入文件，断点续传
  private suspend fun fileAppend(task: DownloadTask, stream: ByteReadChannel) {
    downloadNMM.appendFile(task.filepath, PureStreamBody(stream))
  }

  /**
   * 启动
   */
  suspend fun startDownload(task: DownloadTask) = if (task.paused.isLocked) { // 表示只是短暂的暂停，不用从内存中恢复
    task.paused.unlock()
    task.status.state = DownloadState.Downloading
    task.emitChanged()
    true
  } else { // 触发断点逻辑
    downloadFactory(task)
  }

  /**
   * 暂停⏸️
   */
  suspend fun pauseDownload(task: DownloadTask) {
    if (task.status.state == DownloadState.Downloading) {
      task.status.state = DownloadState.Paused
      task.emitChanged()
      task.paused.tryLock()
      // 暂停并不会删除文件
      downloadStore.set(task.id, task) // 保存到文件
    }
  }

  /**
   * 取消下载
   */
  suspend fun cancelDownload(taskId: TaskId) = downloadTaskMaps[taskId]?.let { downloadTask ->
    // 如果有文件,直接删除
    if (fileExists(downloadTask.filepath)) {
      fileRemove(downloadTask.filepath)
    }
    // 修改状态
    downloadTask.cancel()
    true
  } ?: false

  fun removeDownload(taskId: TaskId) {
    downloadTaskMaps.remove(taskId)?.let { downloadTask ->
      downloadTask.readChannel?.cancel()
      downloadTask.readChannel = null
      downloadNMM.ioAsyncScope.launch { fileRemove(downloadTask.filepath) }
    }
  }

  /**
   * 执行下载任务 ,可能是断点下载
   */
  suspend fun downloadFactory(task: DownloadTask): Boolean =
    when (task.status.state) {
      DownloadState.Init, DownloadState.Failed, DownloadState.Canceled -> {
        doDownload(task) // 执行下载
      }

      DownloadState.Paused -> when (task.readChannel) {
        /// 从磁盘中恢复下载
        null -> doDownload(task)
        else -> {
          task.status.state = DownloadState.Downloading // 这边开始请求http了，属于开始下载
          task.emitChanged()
          true
        }
      }

      DownloadState.Downloading, DownloadState.Completed -> true
    }


  /**
   * 窗口是单例模式
   */
  private var win: WindowController? = null
  suspend fun renderDownloadWindow(wid: UUID) = winLock.withLock {
    downloadNMM.getWindow(wid).also { newWin ->
      if (win == newWin) {
        return@withLock
      }
      win = newWin
      newWin.setStateFromManifest(downloadNMM)
      /// 提供渲染适配
      windowAdapterManager.provideRender(wid) { modifier ->
        Render(modifier, this)
      }
      newWin.onClose {
        winLock.withLock {
          if (newWin == win) {
            win = null
          }
        }
      }
    }
  }

  suspend fun close() = winLock.withLock { downloadNMM.getMainWindow().hide() }
}