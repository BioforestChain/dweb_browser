package org.dweb_browser.browser.download

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.serialization.Serializable
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.file.ext.pickFile
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.DisplayMode
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.fromBase64
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.helper.valueNotIn
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.queryAs
import org.dweb_browser.pure.http.queryAsOrNull
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.onRenderer
import kotlin.time.Duration.Companion.microseconds

internal val debugDownload = Debugger("Download")

class DownloadNMM : NativeMicroModule("download.browser.dweb", "Download") {
  init {
    short_name = BrowserI18nResource.download_shore_name.text
    categories = listOf(
//      MICRO_MODULE_CATEGORY.Application,
      MICRO_MODULE_CATEGORY.Network_Service,
    )
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
    val decodeUrl by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { url.fromBase64().decodeToString() }
  }

  inner class DownloadRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    @OptIn(FlowPreview::class)
    override suspend fun _bootstrap() {
      val controller = DownloadController(this)
      controller.loadDownloadList()
      onBeforeShutdown {
        controller.downloadTaskMaps.suspendForEach { _, downloadTask ->
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
          val downloadTask = controller.createTaskFactory(params, mmid, externalDownload)
          debugDownload("/create", "task=$downloadTask")
          if (params.start) {
            controller.downloadFactory(downloadTask)
          }
          /// TODO 使用新版的 模块文件系统替代 realPath，比如 file:///$mmid/{$downloadTask.filepath}
          downloadTask.filepath = pickFile(downloadTask.filepath).toString()
          downloadTask.toJsonElement()
        },
        // 开始/恢复 下载
        "/start" bind PureMethod.GET by defineBooleanResponse {
          val taskId = request.query("taskId")
          debugDownload("/start", taskId)
          val task = controller.downloadTaskMaps[taskId] ?: return@defineBooleanResponse false
          debugDownload("/start", "task=$task")
          controller.startDownload(task)
        },
        // 监控下载进度
        "/watch/progress" byChannel { ctx ->
          val taskId = request.query("taskId")
          val fps = request.queryAsOrNull<Double>("fps") ?: 10.0
          val throttleMs = (1000.0 / fps).microseconds
          val downloadTask = controller.downloadTaskMaps[taskId]
            ?: return@byChannel close(Throwable("not Found download task!"))
          debugDownload("/watch/progress", "taskId=$taskId")
          callbackFlow {
            ctx.onClose {
              this@callbackFlow.close()
            }
            val off = downloadTask.onChange {
              this@callbackFlow.send(it)
              when (it.status.state) {
                DownloadState.Canceled, DownloadState.Failed, DownloadState.Completed -> {
                  ctx.sendJsonLine(it.status)// 强行发送一帧
                  this@callbackFlow.close()
                }

                else -> {}
              }
            }
            off.removeWhen(onClose)
            downloadTask.emitChanged()
            awaitClose {
              off()
              ctx.close()
            }
          }.conflate().collectIn(mmScope) {
            if (!ctx.isClosed) {
              ctx.sendJsonLine(it.status)
              delay(throttleMs)
            } else {
              WARNING("QAQ ctx.isClosed")
            }
          }
        },
        "/flow/progress" byChannel { ctx ->
          val taskId = request.query("taskId")
          val downloadTask = controller.downloadTaskMaps[taskId]
            ?: return@byChannel close(Throwable("not Found download task!"))
          debugDownload("/flow/progress", "taskId=$taskId")
          var statusValue = downloadTask.status
          var statusWaiter: CompletableDeferred<DownloadStateEvent>? = null
          val off = downloadTask.onChange {
            val stateEvent = it.status.copy()
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
            off()
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
        "/pause" bind PureMethod.GET by defineBooleanResponse {
          val taskId = request.query("taskId")
          val task = controller.downloadTaskMaps[taskId] ?: return@defineBooleanResponse false
          controller.pauseDownload(task)
          true
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
        },
        // taskId是否存在, -1 是不存在，其他返回值是下载的进度
        "/exists" bind PureMethod.GET by defineBooleanResponse {
          val taskId = request.query("taskId")
          debugDownload("exists", "$taskId=>${controller.downloadTaskMaps[taskId]}")
          controller.downloadTaskMaps[taskId]?.status?.state?.valueNotIn(
            DownloadState.Completed, DownloadState.Canceled
          ) ?: false
        },
        // taskId是否存在, -1 是不存在，其他返回值是下载的进度
        "/current" bind PureMethod.GET by defineNumberResponse {
          val taskId = request.query("taskId")
          debugDownload("exists", "$taskId=>${controller.downloadTaskMaps[taskId]}")
          controller.downloadTaskMaps[taskId]?.status?.let { status ->
            if (status.state.valueNotIn(DownloadState.Completed, DownloadState.Canceled)) {
              status.current
            } else -1L
          } ?: -1L
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