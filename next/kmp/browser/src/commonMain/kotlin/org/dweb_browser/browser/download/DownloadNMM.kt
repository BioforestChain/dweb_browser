package org.dweb_browser.browser.download

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.FlowPreview
import kotlinx.serialization.Serializable
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.download.model.DownloadState
import org.dweb_browser.browser.download.model.DownloadStateEvent
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.ResponseException
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.file.ext.pickFile
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.DisplayMode
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.queryAs
import org.dweb_browser.pure.http.queryAsOrNull
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.onRenderer

internal val debugDownload = Debugger("Download")

class DownloadNMM : NativeMicroModule("download.browser.dweb", "Download") {
  init {
    short_name = BrowserI18nResource.download_shore_name.text
    categories = listOf(
      MICRO_MODULE_CATEGORY.Network_Service,
    ).let {
      if (envSwitch.isEnabled(ENV_SWITCH_KEY.BROWSER_DOWNLOAD)) {
        it + MICRO_MODULE_CATEGORY.Application
      } else it
    }
    display = DisplayMode.Fullscreen
    icons =
      listOf(ImageResource(src = "file:///sys/browser-icons/$mmid.svg", type = "image/svg+xml"))
  }

  @Serializable
  data class DownloadTaskParams(
    /** 下载链接 */
    val url: String,
    /** 来源链接 */
    val originUrl: String? = null,
    /** 下载回调链接 */
    val openDappUri: String? = null,
    /** 文件的元数据类型，可以用来做“打开文件”时的参考类型 */
    val mime: String? = null,
    /** 是否直接开始下载(如果您需要监听完整的进度流程，可以先监听再调用下载)*/
    val start: Boolean = false,
    /** 用于接收json中文件大小 */
    val total: Long = 1L,
  ) {
  }

  inner class DownloadRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    @OptIn(FlowPreview::class)
    override suspend fun _bootstrap() {
      val controller = DownloadController(this)
      controller.loadDownloads()
      onBeforeShutdown {
        controller.downloadMap.forEach { (_, downloadTask) ->
          controller.pauseDownload(downloadTask)
        }
      }
      routes(
        // 开始下载
        "/create" bind PureMethod.GET by defineJsonResponse {
          val mmid = ipc.remote.mmid
          val params = request.queryAs<DownloadTaskParams>()
          val externalDownload = request.queryAsOrNull<Boolean>("external") ?: false
          debugDownload("/create", "mmid=$mmid, params=$params, external=$externalDownload")
          // 由于下面会修改downloadTask的filepath，但是在下载时，需要保持真实路径，所以这边做了copy操作
          val downloadTask = controller.createTaskFactory(params, mmid, externalDownload).copy()
          debugDownload("/create", "task=$downloadTask")
          if (params.start) {
            controller.downloadFactory(downloadTask)
          }
          /// TODO 使用新版的 模块文件系统替代 realPath，比如 file:///$mmid/{$downloadTask.filepath}
          downloadTask.filepath = pickFile(downloadTask.filepath)
          downloadTask.toJsonElement()
        },
        // 获取当前的task
        "/getTask" bind PureMethod.GET by defineJsonResponse {
          val taskId = request.queryOrNull("taskId")
            ?: throw ResponseException(HttpStatusCode.BadRequest, "taskId is null")
          debugDownload("exists", "taskId = $taskId")
          controller.downloadMap[taskId]?.copy()?.let { downloadTask ->
            downloadTask.filepath = pickFile(downloadTask.filepath)
            downloadTask.toJsonElement()
          } ?: throw ResponseException(HttpStatusCode.NotFound, "not found task by $taskId")
        },
        // 开始/恢复 下载
        "/start" bind PureMethod.GET by defineBooleanResponse {
          val taskId = request.query("taskId")
          debugDownload("/start", taskId)
          val task = controller.downloadMap[taskId] ?: return@defineBooleanResponse false
          debugDownload("/start", "task=$task")
          controller.startDownload(task)
        },
        // 监控下载进度
        "/flow/progress" byChannel { ctx ->
          val taskId = request.query("taskId")
          val downloadTask = controller.downloadMap[taskId]
            ?: return@byChannel close(Throwable("not Found download task!"))
          debugDownload("/flow/progress", "taskId=$taskId")
          var statusValue = downloadTask.status
          var statusWaiter: CompletableDeferred<DownloadStateEvent>? = null
          val job = downloadTask.onChange.collectIn(mmScope) {
            val stateEvent = downloadTask.status.copy()
            statusValue = stateEvent
            statusWaiter?.also { waiter ->
              statusWaiter = null
              waiter.complete(stateEvent)
            }
            when (stateEvent.state) {
              DownloadState.Canceled, DownloadState.Failed, DownloadState.Completed -> {
                ctx.sendJsonLine(stateEvent)// 直接发送结束帧
                ctx.close()
              }

              else -> {}
            }
          }
          ctx.onClose {
            job.cancel()
          }
          var lastSentValue: Any? = null
          // 同时处理 stateFlow 和 commandChannel
          for (frame in ctx.income) {
            if (frame.text == "get") {
              lastSentValue = if (statusValue === lastSentValue) {
                CompletableDeferred<DownloadStateEvent>().also {
                  statusWaiter = it
                }.await().copy()
              } else {
                statusValue
              }
              ctx.sendJsonLine(lastSentValue)
            }
          }
        },
        // 暂停下载
        "/pause" bind PureMethod.GET by defineJsonResponse {
          val taskId = request.query("taskId")
          val task = controller.downloadMap[taskId]
            ?: throwException(message = "no found taskId=$taskId")
          controller.pauseDownload(task)
          task.status.toJsonElement()
        },
        // 取消下载
        "/cancel" bind PureMethod.GET by defineBooleanResponse {
          val taskId = request.query("taskId")
          controller.cancelDownload(taskId)
        },
        // 移除任务
        "/remove" bind PureMethod.DELETE by defineEmptyResponse {
          val taskId = request.query("taskId")
          controller.removeDownload(taskId)
        })
      onRenderer {
        controller.renderDownloadWindow(wid)
        getMainWindow().setStateFromManifest(manifest)
      }
    }

    override suspend fun _shutdown() {

    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = DownloadRuntime(bootstrapContext)
}