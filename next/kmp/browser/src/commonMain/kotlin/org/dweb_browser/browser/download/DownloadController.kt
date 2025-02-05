package org.dweb_browser.browser.download

import io.ktor.http.ContentRange
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.RangeUnits
import io.ktor.http.Url
import io.ktor.http.fromFilePath
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.writeByteArray
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.download.model.DecompressModel
import org.dweb_browser.browser.download.model.DownloadListModel
import org.dweb_browser.browser.download.model.DownloadState
import org.dweb_browser.browser.download.model.DownloadStateEvent
import org.dweb_browser.browser.download.model.DownloadTask
import org.dweb_browser.browser.download.render.Render
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.FileMetadata
import org.dweb_browser.core.std.file.ext.appendFile
import org.dweb_browser.core.std.file.ext.existFile
import org.dweb_browser.core.std.file.ext.infoFile
import org.dweb_browser.core.std.file.ext.removeFile
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

class DownloadController(internal val downloadNMM: DownloadNMM.DownloadRuntime) {
  private val downloadStore = DownloadStore(downloadNMM)
  internal inline fun <R> launch(crossinline block: suspend () -> R) {
    downloadNMM.scopeLaunch(cancelable = true) { block() }
  }

  val downloadMapFlow = MutableStateFlow(mapOf<String, DownloadTask>())
  var downloadMap
    get() = downloadMapFlow.value
    set(value) {
      downloadMapFlow.value = value
    }


  private var winLock = Mutex(false)
  val downloadListModel = DownloadListModel(this)
  val decompressModel = DecompressModel(this)

  suspend fun loadDownloads() {
    downloadMap = downloadStore.getAll().also { map ->
      map.forEach { (_, downloadTask) ->
        // 如果是从文件中读取的，需要将下载中的状态统一置为暂停。其他状态保持不变
        if (downloadTask.status.state == DownloadState.Downloading) {
          if (fileExists(downloadTask.filepath)) { // 为了保证下载中的状态current值正确
            downloadTask.status.current = fileInfo(downloadTask.filepath).size ?: 0L
          }
          downloadTask.status.state = DownloadState.Paused
        }
      }
    }
    debugDownload("loadDownloads", downloadMap)
  }

  /**
   * 创建新下载任务
   */
  suspend fun createTaskFactory(
    params: DownloadNMM.DownloadTaskParams, originMmid: MMID, externalDownload: Boolean,
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
    task.external = externalDownload // 后面有用到这个字段，这边需要在初始化的时候赋值
    downloadStore.set(task.id, task) // 保存下载状态
    downloadMap += task.id to task
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
    var response = downloadNMM.nativeFetch(
      PureClientRequest(href = task.url, method = PureMethod.GET, headers = PureHeaders().apply {
        init(HttpHeaders.Range, "${RangeUnits.Bytes}=${ContentRange.TailFrom(start)}")
      })
    )
    // 目前发现测试的时候，如果不存在range的上面会报错。直接使用下面这个来请求
    if (response.status == HttpStatusCode.RequestedRangeNotSatisfiable) {
      task.status.current = 0L
      response =
        downloadNMM.nativeFetch(PureClientRequest(href = task.url, method = PureMethod.GET))
    }

    if (!response.isOk) {
      task.status.state = DownloadState.Failed
      task.status.stateMessage = response.status.description
      task.emitChanged()
      downloadNMM.showToast(response.status.toString())
      return false
    }

    task.mime = mimeFactory(response.headers, task.url)
    // TODO 这个本来是考虑如果地址获取的文件名有误，所以才增加的，但是由于改造，创建的时候返回来downloadTask，导致这边修改并没有被捕获，所以暂时移除，待优化
    // task.filepath = fileCreateByHeadersAndPath(response.headers, task.url, task.mime, task.external)

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
    downloadMap += taskId to task
    downloadStore.set(taskId, task)
    // 正式下载需要另外起一个协程，不影响当前的返回值
    downloadNMM.scopeLaunch(cancelable = true) {
      debugDownload("middleware", "start id:$taskId current:${task.status.current}")
      task.emitChanged()
      try {
        streamReader.consumeEachArrayRange { byteArray, last ->
          // 处理是否暂停
          task.paused.withLock {}
          if (byteArray.isNotEmpty()) {
            task.status.current += byteArray.size
            output.writeByteArray(byteArray)
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
        debugDownload("middleware") { "end id:$taskId, ${task.status}" }
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
    downloadNMM.scopeLaunch(cancelable = true) {
      fileAppend(task, output)
    }
    return true
  }

  private fun mimeFactory(headers: PureHeaders, filePath: String): String {
    // 先从header判断
    val contentType = headers.get("Content-Type")
    if (!contentType.isNullOrEmpty()) {
      return contentType
    }
    // 再从文件判断
    val extension = ContentType.fromFilePath(filePath)
    if (extension.isNotEmpty()) {
      return extension.first().toString()
    }
    return "application/octet-stream"
  }

  /**
   * 通过Header来创建不重复的文件
   */
  private suspend fun fileCreateByHeadersAndPath(
    headers: PureHeaders, url: String, mime: String, externalDownload: Boolean,
  ): String {
    // 先从header判断
    var fileName = headers.get("Content-Disposition")?.substringAfter("filename=")?.trim('"')
      ?: Url(url).pathSegments.lastOrNull() ?: ""
    if (fileName.isEmpty()) fileName = "${datetimeNow()}.${mime.substringAfter("/")}"
    var index = 0
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

  /**
   * 创建不重复的文件
   */
  private suspend fun fileCreateByPath(url: String, externalDownload: Boolean): String {
    var index = 0
    val fileName = Url(url).pathSegments.lastOrNull() ?: ""
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
  suspend fun cancelDownload(taskId: TaskId) = downloadMap[taskId]?.let { downloadTask ->
    // 如果有文件,直接删除
    if (fileExists(downloadTask.filepath)) {
      fileRemove(downloadTask.filepath)
    }
    // 修改状态
    downloadTask.cancel()
    true
  } ?: false

  suspend fun removeDownload(taskId: TaskId) {
    downloadMap[taskId]?.let { downloadTask ->
      downloadTask.readChannel?.cancel()
      downloadTask.readChannel = null

      downloadMap -= taskId
      downloadStore.delete(taskId)
      fileRemove(downloadTask.filepath)
    }
  }

  /**
   * 执行下载任务 ,可能是断点下载
   */
  suspend fun downloadFactory(task: DownloadTask): Boolean = when (task.status.state) {
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

  suspend fun close() = winLock.withLock { downloadNMM.getMainWindow().tryCloseOrHide() }
}