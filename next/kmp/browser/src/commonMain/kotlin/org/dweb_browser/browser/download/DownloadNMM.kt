package org.dweb_browser.browser.download

import io.ktor.http.ContentRange
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.fromFilePath
import io.ktor.http.headers
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureStreamBody
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.helper.IpcHeaders
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.FileMetadata
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.sys.window.core.helper.setFromManifest
import org.dweb_browser.sys.window.core.onRenderer
import org.dweb_browser.sys.window.ext.getMainWindow

internal val debugDownload = Debugger("Download")

class DownloadNMM : NativeMicroModule("download.browser.dweb", "Download") {
  init {
    short_name = "下载管理"
    categories = listOf(
      MICRO_MODULE_CATEGORY.Application,
      MICRO_MODULE_CATEGORY.Network_Service,
    )
    icons = listOf(ImageResource(src = "file:///sys/icons/$mmid.svg", type = "image/svg+xml"))
  }

  @Serializable
  data class DownloadTaskParams(
    /** 下载链接 */
    val url: String,
    /** 来源链接 */
    val originUrl: String? = null,
    /** 下载回调链接 */
    val completeCallbackUrl: String? = null,
    /** 文件的元数据类型，可以用来做“打开文件”时的参考类型 */
    val mime: String? = null,
    /** 是否直接开始下载(如果您需要监听完整的进度流程，可以先监听再调用下载)*/
    val start: Boolean = false
  )

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val controller = DownloadController(this)
    onAfterShutdown {
      for (task in controller.downloadManagers) {
        task.value.pause()
      }
    }
    routes(
      // 开始下载
      "/create" bind HttpMethod.Get to defineStringResponse {
        val mmid = ipc.remote.mmid
        val params = request.queryAs<DownloadTaskParams>()
        val downloadTask = createTaskFactory(controller, params, mmid)
        debugDownload("/create", "mmid=$mmid, taskId=$downloadTask, params=$params")
        if (params.start) {
          controller.downloadFactory(downloadTask)
        }
        downloadTask.id
      },
      // 开始/恢复 下载
      "/start" bind HttpMethod.Get to defineBooleanResponse {
        val taskId = request.query("taskId")
        debugDownload("/start", "$taskId -> ${controller.downloadManagers[taskId]}")
        val task = controller.downloadManagers[taskId] ?: return@defineBooleanResponse false
        // 表示只是短暂的暂停，不用从内存中恢复
        if (task.pauseFlag) {
          task.paused.resolve(Unit)
          return@defineBooleanResponse true
        }
        // 触发断点逻辑
        controller.downloadFactory(task)
      },
      // 监控下载进度
      "/watch/progress" bind HttpMethod.Get to defineJsonLineResponse {
        val taskId = request.query("taskId")
        val downloadTask = controller.downloadManagers[taskId]
          ?: return@defineJsonLineResponse emit("not Found download task!")
        debugDownload("/watch/progress", "taskId=$taskId")
        // 给别人的需要给picker地址
        val pickFilepath =
          nativeFetch("file://file.std.dweb/picker?path=${downloadTask.filepath}").text()
        downloadTask.onDownload {
          emit(it.copy(filepath = pickFilepath))
        }
        downloadTask.downloadSignal.emit(downloadTask)
      },
      // 暂停下载
      "/pause" bind HttpMethod.Get to defineBooleanResponse {
        val taskId = request.query("taskId")
        val task = controller.downloadManagers[taskId] ?: return@defineBooleanResponse false
        task.pause()
        true
      },
      // 取消下载
      "/cancel" bind HttpMethod.Get to defineBooleanResponse {
        val taskId = request.query("taskId")
        val task = controller.downloadManagers[taskId] ?: return@defineBooleanResponse false
        task.cancel()
        controller.downloadManagers.remove(taskId)?.let {
          it.status.state = DownloadState.Canceled
          controller.downloadCompletes[taskId] = it
        }
        true
      },
      // 移除任务
      "/remove" bind HttpMethod.Delete to defineEmptyResponse {
        val taskId = request.query("taskId")
        controller.downloadManagers.remove(taskId)
      },
    )
    onRenderer {
      controller.renderDownloadWindow(wid)
      getMainWindow().state.apply {
        setFromManifest(this@DownloadNMM)
      }
    }
  }

  /**
   * 创建新下载任务
   */
  private suspend fun createTaskFactory(
    controller: DownloadController, params: DownloadTaskParams, originMmid: MMID
  ): DownloadTask {
    // 查看是否创建过相同的task,并且相同的task已经下载完成
    val task = DownloadTask(
      id = randomUUID(),
      url = params.url,
      createTime = datetimeNow(),
      originMmid = originMmid,
      originUrl = params.originUrl,
      completeCallbackUrl = params.completeCallbackUrl,
      mime = "application/octet-stream",
      filepath = createFilePath(params.url)
    )
    recover(task, ContentRange.TailFrom(0L), controller)
    controller.downloadManagers[task.id] = task
    debugDownload("初始化成功！", "${task.id} -> $task")
    return task
  }

  /**
   * 恢复(创建)下载，需要重新创建连接🔗
   */
  suspend fun recover(task: DownloadTask, range: ContentRange, controller: DownloadController) {
    val response = nativeFetch(URLBuilder(task.url).also {
      headers { append(HttpHeaders.Range, range.toString()) }
    }.buildString())
    // 直接变成失败
    task.mime = mimeFactory(response.headers, task.url)
    if (!response.isOk()) {
      task.status.state = DownloadState.Failed
      task.status.stateMessage = response.text()
      controller.downloadManagers.remove(task.id)?.let { // 下载失败，转移到已完成列表
        controller.downloadCompletes[task.id] = it
      }
    } else {
      // 下载流程初始化成功
      task.status.state = DownloadState.Init
      task.status.total = response.headers.get("Content-Length")?.toLong() ?: 1L
      task.readChannel = response.stream().getReader("downloadTask#${task.id}")
    }
  }

  /**
   * 暂停⏸️
   */
  fun DownloadTask.pause() {
    // 暂停并不会删除文件
    this.status.state = DownloadState.Paused
    this.pauseFlag = true
    // 并不需要每次都从内存中恢复
  }

  /**
   * 取消下载
   */
  suspend fun DownloadTask.cancel() {
    // 如果有文件,直接删除
    if (exist(this.filepath)) {
      remove(this.filepath)
    }
    // 修改状态
    val channel = this.readChannel
    this.status.state = DownloadState.Canceled
    this.status.current = 0L
    channel?.let {
      it.cancel()
    }
    this.readChannel = null
  }

  /**
   * 创建不重复的文件
   */
  private suspend fun createFilePath(url: String): String {
    var index = 0
    var path: String
    val fileName = url.substring(url.lastIndexOf("/") + 1)
    do {
      path = "/data/download/${index++}_${fileName}"
      val boolean = exist(path)
    } while (boolean)
    return path
  }

  private fun mimeFactory(header: IpcHeaders, filePath: String): String {
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

  suspend fun exist(path: String): Boolean {
    val response = nativeFetch("file://file.std.dweb/exist?path=$path")
    return response.boolean()
  }

  suspend fun info(path: String): FileMetadata {
    val response = nativeFetch("file://file.std.dweb/info?path=$path")
    return Json.decodeFromString(response.text())
  }

  suspend fun remove(filepath: String): Boolean {
    return nativeFetch(
      PureRequest(
        "file://file.std.dweb/remove?path=${filepath}&recursive=true", IpcMethod.DELETE
      )
    ).boolean()
  }

  //  追加写入文件，断点续传
  suspend fun appendFile(task: DownloadTask, stream: ByteReadChannel) {
    nativeFetch(
      PureRequest(
        "file://file.std.dweb/append?path=${task.filepath}&create=true",
        IpcMethod.PUT,
        body = PureStreamBody(stream)
      )
    )
  }

  override suspend fun _shutdown() {

  }
}