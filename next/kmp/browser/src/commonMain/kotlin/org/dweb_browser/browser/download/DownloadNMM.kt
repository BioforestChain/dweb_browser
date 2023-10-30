package org.dweb_browser.browser.download

import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.fromFilePath
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureResponse
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

  /**
   * 用来记录文件是否被下载完成，用来做断点续传
   * 1. 下载到一半task state 还在 download 则不会创建新的downloadTask，用之前的继续写入
   * 2. 已经完成下载了，再次创建个新的Task继续下载
   * 3. 当前同一url文件，只能等上一个task任务下载完成后，才能继续创建task下载相同的文件，不然同时开多个task下载同一文件到同一个地方是没有意义的
   */
  // private val downloadMap = mutableMapOf<String, DownloadTask>()

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
      controller.downloadManagers.clear()
    }
    routes(
      "/running" bind HttpMethod.Get to defineBooleanResponse {
        request.queryOrNull("taskId")?.let { taskId ->
          controller.downloadManagers[taskId]?.let { downloadTask ->
            // 如果状态是正在下载，或者暂停状态，即为正在下载，可以创建下载工程，其余全部忽略
            if (downloadTask.status.state == DownloadState.Downloading ||
              downloadTask.status.state == DownloadState.Paused
            ) {
              downloadTask.createTaskFactory(controller)
              true
            } else false
          }
        } ?: false
      },
      // 开始下载
      "/create" bind HttpMethod.Get to defineStringResponse {
        val mmid = ipc.remote.mmid
        val params = request.queryAs<DownloadTaskParams>()
        val downloadTask = createTaskFactory(controller, params, mmid)
        debugDownload("/create", "mmid=$mmid, taskId=$downloadTask, params=$params")
        if (params.start) {
          downloadFactory(controller, downloadTask)
        }
        downloadTask.id
      },
      // 开始/恢复 下载
      "/start" bind HttpMethod.Get to defineBooleanResponse {
        val taskId = request.query("taskId")
        debugDownload("/start", "$taskId -> ${controller.downloadManagers[taskId]}")
        val task = controller.downloadManagers[taskId] ?: return@defineBooleanResponse false
        downloadFactory(controller, task)
      },
      // 监控下载进度
      "/watch/progress" bind HttpMethod.Get to defineJsonLineResponse {
        val taskId = request.query("taskId")
        val downloadTask = controller.downloadManagers[taskId]
          ?: return@defineJsonLineResponse emit("not Found download task!")
        debugDownload("/watch/progress", "taskId=$taskId")
        downloadTask.onDownload {
          emit(it)
        }
        downloadTask.downloadSignal.emit(downloadTask)
      },
      // 暂停下载
      "/pause" bind HttpMethod.Get to defineBooleanResponse {
        val taskId = request.query("taskId")
        val task = controller.downloadManagers[taskId] ?: return@defineBooleanResponse false
        val readChannel = task.readChannel ?: return@defineBooleanResponse false
        readChannel.cancel()
        true
      },
      // 取消下载
      "/cancel" bind HttpMethod.Get to defineBooleanResponse {
        val taskId = request.query("taskId")
        val task = controller.downloadManagers[taskId] ?: return@defineBooleanResponse false
        val channel = task.readChannel ?: return@defineBooleanResponse false
        channel.cancel()
        true
      },
    )
    onRenderer {
      getMainWindow().state.apply {
        setFromManifest(this@DownloadNMM)
      }
    }
  }

  fun DownloadTask.pause() {}

  /**
   * 如果 DownloadNMM 中找到了taskId对应的 DownloadTask,那么就可以针对当前的task进行创建下载链接
   */
  private suspend fun DownloadTask.createTaskFactory(
    controller: DownloadController, resp: PureResponse? = null
  ) {
    if (this.readChannel != null) return // 如果该对象已存在，表示可以下载，就不知幸下面的操作
    val response = resp ?: nativeFetch(url)
    // 直接变成失败
    if (!response.isOk()) {
      status.state = DownloadState.Failed
      status.stateMessage = response.text()
    } else {
      // 下载流程初始化成功
      status.state = DownloadState.Init
      status.total = response.headers.get("Content-Length")?.toLong() ?: 1L
      readChannel = response.stream().getReader("downloadTask#${id}")
    }
    controller.downloadManagers[id] = this
    debugDownload("初始化成功！", "$id -> $this")
  }

  private suspend fun createTaskFactory(
    controller: DownloadController, params: DownloadTaskParams, originMmid: MMID
  ): DownloadTask {
    val response = nativeFetch(params.url)
    // 查看是否创建过相同的task,并且相同的task已经下载完成
    val task = DownloadTask(
      id = randomUUID(),
      url = params.url,
      createTime = datetimeNow(),
      originMmid = originMmid,
      originUrl = params.originUrl,
      completeCallbackUrl = params.completeCallbackUrl,
      mime = mimeFactory(response.headers, params.url),
      filepath = createFlePath(params.url),
    )
    // task.createTaskFactory(controller, response)
    // 直接变成失败
    if (!response.isOk()) {
      task.status.state = DownloadState.Failed
      task.status.stateMessage = response.text()
    } else {
      // 下载流程初始化成功
      task.status.state = DownloadState.Init
      task.status.total = response.headers.get("Content-Length")?.toLong() ?: 1L
      task.readChannel = response.stream().getReader("downloadTask#${task.id}")
    }
    controller.downloadManagers[task.id] = task
    debugDownload("初始化成功！", "${task.id} -> $task")
    return task
  }

  /**
   * 创建不重复的文件
   */
  private suspend fun createFlePath(url: String): String {
    var index = 0
    var path: String
    val fileName = url.substring(url.lastIndexOf("/") + 1)
    do {
      path = "/data/download/${index++}_${fileName}"
      val boolean = exist(path)
    } while (boolean)
    return nativeFetch("file://file.std.dweb/picker?path=${path}").text()
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

  private suspend fun exist(path: String): Boolean {
    val response = nativeFetch("file://file.std.dweb/exist?path=$path")
    return response.boolean()
  }

  private suspend fun info(path: String): FileMetadata {
    val response = nativeFetch("file://file.std.dweb/info?path=$path")
    return Json.decodeFromString(response.text())
  }

  //  追加写入文件，断点续传
  private suspend fun appendFile(task: DownloadTask, stream: ByteReadChannel) {
    nativeFetch(
      PureRequest(
        "file://file.std.dweb/append?path=${task.filepath}&create=true",
        IpcMethod.PUT,
        body = PureStreamBody(stream)
      )
    )
  }

  private suspend fun downloadFactory(controller: DownloadController, task: DownloadTask): Boolean {
    val stream = task.readChannel ?: return false
    debugDownload("downloadFactory", task.id)
    // 已经存在了从断点开始
    if (exist(task.filepath)) {
      val current = info(task.filepath).size
      // 当前进度
      current?.let {
        task.status.current = it
      }
    }
    val buffer = controller.middleware(task, stream)
    appendFile(task, buffer)
    return true
  }

  override suspend fun _shutdown() {

  }
}