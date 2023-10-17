package org.dweb_browser.browser.download

//import org.dweb_browser.core.module.getAppContext
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.fromFilePath
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileMetadata
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureStreamBody
import org.dweb_browser.core.http.bind
import org.dweb_browser.core.ipc.helper.IpcHeaders
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.sys.window.core.onRenderer

internal val debugDownload = Debugger("Download")
private typealias downloadUrl = String

class DownloadNMM : NativeMicroModule("download.browser.dweb", "Download") {
  init {
    short_name = "下载管理"
    categories = listOf(
      MICRO_MODULE_CATEGORY.Application,
      MICRO_MODULE_CATEGORY.Network_Service,
      MICRO_MODULE_CATEGORY.Application,
    )
    icons = listOf(ImageResource(src = "file:///sys/icons/$mmid.svg", type = "image/svg+xml"))
//    // 初始化下载适配器
//    fileTypeAdapterManager.append(
//      adapter = commonVirtualFsDirectoryFactory(
//        "download",
//        getAppContext().dataDir.absolutePath.toPath()
//      )
//    )
  }

  private val controller = DownloadController(this)

  /**
   * 用来记录文件是否被下载完成，用来做断点续传
   * 1. 下载到一半task state 还在 download 则不会创建新的downloadTask，用之前的继续写入
   * 2. 已经完成下载了，再次创建个新的Task继续下载
   * 3. 当前同一url文件，只能等上一个task任务下载完成后，才能继续创建task下载相同的文件，不然同时开多个task下载同一文件到同一个地方是没有意义的
   */
  private val downloadMap = mutableMapOf<downloadUrl, DownloadTask>()

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
    onAfterShutdown {
      for (task in controller.downloadManagers) {
        task.value.pause()
      }
      controller.downloadManagers.clear()
      downloadMap.clear()
    }
    routes(
      // 开始下载
      "/create" bind HttpMethod.Get to defineStringResponse {
        val mmid = ipc.remote.mmid
        debugDownload("/create", mmid)
        val params = request.queryAs<DownloadTaskParams>()
        val task = createTaskFactory(params, mmid)
        if (params.start) {
          controller.downloadFactory(task)
        }
        task.id
      },
      // 开始/恢复 下载
      "/start" bind HttpMethod.Get to defineBooleanResponse {
        val taskId = request.query("taskId")
        val task = controller.downloadManagers[taskId]
          ?: return@defineBooleanResponse false
        controller.downloadFactory(task)
      },
      // 监控下载进度
      "/watch/progress" bind HttpMethod.Get to defineJsonLineResponse {
        val taskId = request.query("taskId")
        val downloadTask = controller.downloadManagers[taskId]
          ?: return@defineJsonLineResponse emit("not Found download task!")
        debugDownload("/watch/progress", "taskId=$taskId ${downloadTask.emitQueue.size}")
        downloadTask.onDownload {
          emit(it)
        }
        downloadTask.downloadSignal.emit(downloadTask)
      },
      // 暂停下载
      "/pause" bind HttpMethod.Get to defineBooleanResponse {
        val taskId = request.query("taskId")
        val task = controller.downloadManagers[taskId]
          ?: return@defineBooleanResponse false
        val readChannel = task.readChannel ?: return@defineBooleanResponse  false
        readChannel.cancel()
        true
      },
      // 取消下载
      "/cancel" bind HttpMethod.Get to defineBooleanResponse {
        val taskId = request.query("taskId")
        val task = controller.downloadManagers[taskId]
          ?: return@defineBooleanResponse false
        val pureStream = task.readChannel ?: return@defineBooleanResponse  false
//        pureStream.getReader("taskId:${task.id}").cancel()
        true
      },
    )
    onRenderer {

    }
  }

  private suspend fun createTaskFactory(
    params: DownloadTaskParams,
    originMmid: MMID
  ): DownloadTask {
    val url = params.url
    // 验证response ok
    val response = nativeFetch(params.url)
    // 查看是否创建过相同的task,并且相同的task已经下载完成
    val task = DownloadTask(
      id = randomUUID(),
      url = url,
      createTime = datetimeNow(),
      originMmid = originMmid,
      originUrl = params.originUrl,
      completeCallbackUrl = params.completeCallbackUrl,
      mime = mimeFactory(response.headers, url),
      filepath = createFlePath(url),
      readChannel = null
    )
    // 直接变成失败
    if (!response.isOk()) {
      task.status = DownloadStateEvent(
        state = DownloadState.Failed
      )
      task.status.stateMessage = response.text()
    } else {
      // 下载流程初始化成功
      task.status = DownloadStateEvent(
        state = DownloadState.Init,
        total = response.headers.get("content-length")?.toLong() ?: 1L
      )
//      task.readChannel = t
    }
    // 存储到任务管理器
    controller.downloadManagers[task.id] = task
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

  fun DownloadTask.pause() {

  }

  override suspend fun _shutdown() {

  }
}