package org.dweb_browser.browser.download

import kotlinx.coroutines.launch
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
import org.dweb_browser.helper.fromBase64
import org.dweb_browser.helper.valueNotIn
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
//      MICRO_MODULE_CATEGORY.Application,
      MICRO_MODULE_CATEGORY.Network_Service,
    )
    display = DisplayMode.Fullscreen
    icons = listOf(ImageResource(src = "file:///sys/icons/$mmid.svg", type = "image/svg+xml"))
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

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val controller = DownloadController(this)
    controller.loadDownloadList()
    onAfterShutdown {
      ioAsyncScope.launch {
        controller.downloadTaskMaps.suspendForEach { _, downloadTask ->
          controller.pauseDownload(downloadTask)
        }
      }
    }
    routes(
      // 开始下载
      "/create" bind PureMethod.GET by defineStringResponse {
        val mmid = ipc.remote.mmid
        val params = request.queryAs<DownloadTaskParams>()
        val externalDownload = request.queryAsOrNull<Boolean>("external") ?: false
        debugDownload("/create", "mmid=$mmid, params=$params, external=$externalDownload")
        val downloadTask = controller.createTaskFactory(params, mmid, externalDownload)
        debugDownload("/create", "task=$downloadTask")
        if (params.start) {
          controller.downloadFactory(downloadTask)
        }
        downloadTask.id
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
        val downloadTask = controller.downloadTaskMaps[taskId]
          ?: return@byChannel close(Throwable("not Found download task!"))
        debugDownload("/watch/progress", "taskId=$taskId")
        // 给别人的需要给picker地址
        val pickFilepath = pickFile(downloadTask.filepath)
        downloadTask.onChange {
          ctx.sendJsonLine(it.copy(filepath = pickFilepath))
        }.removeWhen(onClose)
        downloadTask.emitChanged()
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
      }
    )
    onRenderer {
      controller.renderDownloadWindow(wid)
      getMainWindow().setStateFromManifest(this@DownloadNMM)
    }
  }

  override suspend fun _shutdown() {

  }
}