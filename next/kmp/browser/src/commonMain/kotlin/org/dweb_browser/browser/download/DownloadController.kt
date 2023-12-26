package org.dweb_browser.browser.download

import androidx.compose.runtime.mutableStateListOf
import io.ktor.http.ContentRange
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.fromFilePath
import io.ktor.http.headers
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
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureStreamBody
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.FileMetadata
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.consumeEachArrayRange
import org.dweb_browser.helper.createByteChannel
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.constant.WindowMode
import org.dweb_browser.sys.window.core.helper.setFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.core.windowInstancesManager
import org.dweb_browser.sys.window.ext.getMainWindow

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
  val status: DownloadStateEvent
) {

  @Transient
  var readChannel: ByteReadChannel? = null

  // 监听下载进度 不存储到内存
  @Transient
  val downloadSignal: Signal<DownloadTask> = Signal()

  @Transient
  val onDownload = downloadSignal.toListener()

  // 帮助实现下载暂停
  @Transient
  var paused = PromiseOut<Unit>()

  @Transient
  var pauseFlag = false
  suspend fun pauseWait() {
    if (pauseFlag) {
      debugDownload("DownloadTask", "下载暂停🚉${this.id}  ${this.status.current}")
      // 触发状态更新
      this.downloadSignal.emit(this)
      paused.waitPromise()
      // 还原状态
      this.status.state = DownloadState.Downloading
      paused = PromiseOut()
      pauseFlag = false
      debugDownload("DownloadTask", "下载恢复🍅")
    }
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
)

class DownloadController(private val downloadNMM: DownloadNMM) {
  private val store = DownloadStore(downloadNMM)
  val downloadManagers: ChangeableMutableMap<TaskId, DownloadTask> =
    ChangeableMutableMap() // 用于监听下载列表
  val downloadList: MutableList<DownloadTask> = mutableStateListOf()
  private var winLock = Mutex(false)
  val downloadModel = DownloadModel(this)
  val decompressModel = DecompressModel(this)

  init {
    // 从内存中恢复状态
    downloadNMM.ioAsyncScope.launch {
      // 状态改变的时候存储保存到内存
      downloadManagers.onChange { (type, _, value) ->
        when (type) {
          ChangeableType.Add -> {
            store.set(value!!.id, value)
            downloadList.add(0, value)
          }

          ChangeableType.Remove -> {
            store.delete(value!!.id)
            downloadList.remove(value)
          }

          ChangeableType.PutAll -> {
            downloadList.addAll(
              downloadManagers.toMutableList().sortedByDescending { it.createTime }
            )
          }

          ChangeableType.Clear -> {
            downloadList.clear()
          }
        }
      }

      downloadManagers.putAll(store.getAll())
      // 如果是从文件中读取的，需要将下载中的状态统一置为暂停。其他状态保持不变
      downloadManagers.suspendForEach { _, downloadTask ->
        if (downloadTask.status.state == DownloadState.Downloading) {
          if (fileExists(downloadTask.filepath)) { // 为了保证下载中的状态current值正确
            downloadTask.status.current = fileInfo(downloadTask.filepath).size ?: 0L
          }
          downloadTask.status.state = DownloadState.Paused
        }
        downloadTask.pauseFlag = false
      }
    }
  }

  /**
   * 创建新下载任务
   */
  suspend fun createTaskFactory(
    params: DownloadNMM.DownloadTaskParams, originMmid: MMID
  ): DownloadTask {
    // 查看是否创建过相同的task,并且相同的task已经下载完成
    val task = DownloadTask(
      id = randomUUID(),
      url = params.url,
      originMmid = originMmid,
      originUrl = params.originUrl,
      openDappUri = params.openDappUri,
      mime = "application/octet-stream",
      filepath = fileCreateByPath(params.url),
      status = DownloadStateEvent(total = params.total)
    )
    recover(task, 0L)
    downloadManagers.put(task.id, task)
    debugDownload("createTaskFactory", "${task.id} -> $task")
    return task
  }

  /**
   * 恢复(创建)下载，需要重新创建连接🔗
   */
  private suspend fun recover(task: DownloadTask, start: Long) {
    debugDownload("recover", start)
    val response = downloadNMM.nativeFetch(URLBuilder(task.url).also {
      headers { append(HttpHeaders.Range, ContentRange.TailFrom(start).toString()) }
    }.buildString())
    // 直接变成失败
    task.mime = mimeFactory(response.headers, task.url)
    if (!response.isOk()) {
      task.status.state = DownloadState.Failed
      task.status.stateMessage = response.status.description
      downloadNMM.nativeFetch("file://toast.sys.dweb/show?message=${response.status}")
    } else {
      // 下载流程初始化成功
      task.status.state = DownloadState.Init
      response.headers.get("Content-Length")?.toLong()?.let { total ->
        debugDownload("recover", "content-length=$total")
        task.status.current = start
        task.status.total = total + start
        // 使用 total 和 task的total进行比对
      } ?: kotlin.run {
        // TODO 如果识别不到Content-Length，目前当做是无法进行ContentRange操作
        task.status.current = 0L
      }
      task.readChannel = response.stream().getReader("downloadTask#${task.id}")
    }
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
  private suspend fun fileCreateByPath(url: String): String {
    var index = 0
    val fileName = url.substring(url.lastIndexOf("/") + 1)
    while (true) {
      val path = "/data/download/${index++}_${fileName}"
      if (!fileExists(path)) {
        return path
      }
    }
  }

  private suspend fun fileExists(path: String): Boolean {
    val response = downloadNMM.nativeFetch("file://file.std.dweb/exist?path=$path")
    return response.boolean()
  }

  private suspend fun fileInfo(path: String): FileMetadata {
    val response = downloadNMM.nativeFetch("file://file.std.dweb/info?path=$path")
    return Json.decodeFromString(response.text())
  }

  private suspend fun fileRemove(filepath: String): Boolean {
    return downloadNMM.nativeFetch(
      PureClientRequest(
        "file://file.std.dweb/remove?path=${filepath}&recursive=true", PureMethod.DELETE
      )
    ).boolean()
  }

  //  追加写入文件，断点续传
  private suspend fun fileAppend(task: DownloadTask, stream: ByteReadChannel) {
    downloadNMM.nativeFetch(
      PureClientRequest(
        "file://file.std.dweb/append?path=${task.filepath}&create=true",
        PureMethod.PUT,
        body = PureStreamBody(stream)
      )
    )
  }

  /**
   * 启动
   */
  suspend fun startDownload(task: DownloadTask) = if (task.pauseFlag) { // 表示只是短暂的暂停，不用从内存中恢复
    task.paused.resolve(Unit)
    true
  } else { // 触发断点逻辑
    downloadFactory(task)
  }

  /**
   * 暂停⏸️
   */
  suspend fun pauseDownload(task: DownloadTask) {
    // 暂停并不会删除文件
    task.status.state = DownloadState.Paused
    task.pauseFlag = true
    store.set(task.id, task) // 保存到文件
  }

  /**
   * 取消下载
   */
  suspend fun cancelDownload(taskId: TaskId) = downloadManagers.get(taskId)?.let { downloadTask ->
    // 如果有文件,直接删除
    if (fileExists(downloadTask.filepath)) {
      fileRemove(downloadTask.filepath)
    }
    // 修改状态
    downloadTask.status.state = DownloadState.Canceled
    downloadTask.status.current = 0L
    downloadTask.readChannel?.cancel()
    downloadTask.readChannel = null
    true
  } ?: false

  fun removeDownload(taskId: TaskId) {
    downloadManagers.remove(taskId)?.let { downloadTask ->
      downloadTask.readChannel?.cancel()
      downloadTask.readChannel = null
    }
  }

  /**
   * 执行下载任务 ,可能是断点下载
   */
  suspend fun downloadFactory(task: DownloadTask): Boolean {
    if (fileExists(task.filepath)) {
      // 已经存在了，并且对方支持range 从断点开始
      val current = fileInfo(task.filepath).size
      debugDownload("downloadFactory", "是否支持range:$current")

      // 已经存在并且下载完成
      if (current != null) {
        // 开始断点续传，这是在内存中恢复的，创建了一个新的channel
        recover(task, current)
        // task.status.current = current
        // 恢复状态 改状态为暂停，并且卡住
        // task.status.state = DownloadState.Paused
        // task.pauseFlag = true
        // task.pauseWait()
      }
    }
    // 如果内存中没有，或者对方不支持Range，需要重新下载,否则这个channel是从支持的断点开始
    val stream = task.readChannel ?: return false
    debugDownload("downloadFactory", task.id)
    val buffer = middleware(task, stream)
    fileAppend(task, buffer)
    return true
  }

  /**
   * 下载 task 中间件
   */
  private fun middleware(downloadTask: DownloadTask, input: ByteReadChannel): ByteReadChannel {
    val output = createByteChannel()
    downloadTask.status.state = DownloadState.Downloading
    val taskId = downloadTask.id
    // 重要记录点 存储到硬盘
    downloadManagers.put(taskId, downloadTask)
    downloadNMM.ioAsyncScope.launch {
      debugDownload("middleware", "start id:$taskId current:${downloadTask.status.current}")
      downloadTask.downloadSignal.emit(downloadTask)
      try {
        input.consumeEachArrayRange { byteArray, last ->
          // 处理是否暂停
          downloadTask.pauseWait()
          if (output.isClosedForRead) {
            breakLoop()
            downloadTask.status.state = DownloadState.Canceled
            downloadTask.status.current = 0L
            // 触发取消 存储到硬盘
            input.cancel()
            store.set(downloadTask.id, downloadTask)
            downloadTask.downloadSignal.emit(downloadTask)
          } else if (last) {
            output.close()
            input.cancel()
            downloadTask.status.state = DownloadState.Completed
            // 触发完成 存储到硬盘
            store.set(downloadTask.id, downloadTask)
            downloadTask.downloadSignal.emit(downloadTask)
          } else {
            downloadTask.status.current += byteArray.size
            // 触发进度更新
            downloadTask.downloadSignal.emit(downloadTask)
            output.writePacket(ByteReadPacket(byteArray))
          }
          debugDownload("middleware", "progress id:$taskId current:${downloadTask.status.current}")
        }
        debugDownload("middleware", "end id:$taskId")
      } catch (e: Throwable) {
        // 这里捕获的一般是 connection reset by peer 当前没有重试机制，用户再次点击即为重新下载
        debugDownload("middleware", "${e.message}")
        downloadTask.status.state = DownloadState.Failed
        // 触发失败
        downloadTask.downloadSignal.emit(downloadTask)
      }
    }
    return output
  }

  /**
   * 窗口是单例模式
   */
  private var win: WindowController? = null
  suspend fun renderDownloadWindow(wid: UUID) = winLock.withLock {
    (windowInstancesManager.get(wid) ?: throw Exception("invalid wid: $wid")).also { newWin ->
      if (win == newWin) {
        return@withLock
      }
      win = newWin
      newWin.state.apply {
        mode = WindowMode.MAXIMIZE
        setFromManifest(downloadNMM)
      }
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