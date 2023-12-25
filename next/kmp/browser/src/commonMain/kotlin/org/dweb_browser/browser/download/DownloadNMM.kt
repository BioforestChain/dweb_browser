package org.dweb_browser.browser.download

import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.queryAs
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.sys.window.core.helper.setFromManifest
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.onRenderer

internal val debugDownload = Debugger("Download")

class DownloadNMM : NativeMicroModule("download.browser.dweb", "Download") {
  init {
    short_name = "下载管理"
    categories = listOf(
//      MICRO_MODULE_CATEGORY.Application,
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
    val openDappUri: String? = null,
    /** 文件的元数据类型，可以用来做“打开文件”时的参考类型 */
    val mime: String? = null,
    /** 是否直接开始下载(如果您需要监听完整的进度流程，可以先监听再调用下载)*/
    val start: Boolean = false,
    /** 用于接收json中文件大小 */
    val total: Long = 1L,
  )

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val controller = DownloadController(this)
    onAfterShutdown {
      ioAsyncScope.launch {
        controller.downloadManagers.suspendForEach { _, downloadTask ->
          controller.pauseDownload(downloadTask)
        }
      }
    }
    routes(
      // 开始下载
      "/create" bind IpcMethod.GET by defineStringResponse {
        val mmid = ipc.remote.mmid
        val params = request.queryAs<DownloadTaskParams>()
        val downloadTask = controller.createTaskFactory(params, mmid)
        debugDownload("/create", "mmid=$mmid, taskId=$downloadTask, params=$params")
        if (params.start) {
          controller.downloadFactory(downloadTask)
        }
        downloadTask.id
      },
      // 开始/恢复 下载
      "/start" bind IpcMethod.GET by defineBooleanResponse {
        val taskId = request.query("taskId")
        debugDownload("/start", taskId)
        val task = controller.downloadManagers.get(taskId) ?: return@defineBooleanResponse false
        controller.startDownload(task)
      },
      // 监控下载进度
      "/watch/progress" byChannel { ctx ->
        val taskId = request.query("taskId")
        val downloadTask = controller.downloadManagers.get(taskId)
          ?: return@byChannel close(Throwable("not Found download task!"))
        debugDownload("/watch/progress", "taskId=$taskId")
        // 给别人的需要给picker地址
        val pickFilepath =
          nativeFetch("file://file.std.dweb/picker?path=${downloadTask.filepath}").text()
        downloadTask.onDownload {
          ctx.sendJsonLine(it.copy(filepath = pickFilepath))
        }.removeWhen(onClose)
        downloadTask.downloadSignal.emit(downloadTask)
      },
      // 暂停下载
      "/pause" bind IpcMethod.GET by defineBooleanResponse {
        val taskId = request.query("taskId")
        val task = controller.downloadManagers.get(taskId) ?: return@defineBooleanResponse false
        controller.pauseDownload(task)
        true
      },
      // 取消下载
      "/cancel" bind IpcMethod.GET by defineBooleanResponse {
        val taskId = request.query("taskId")
        controller.cancelDownload(taskId)
      },
      // 移除任务
      "/remove" bind IpcMethod.DELETE by defineEmptyResponse {
        val taskId = request.query("taskId")
        controller.removeDownload(taskId)
      },
      // taskId是否存在
      "/exists" bind IpcMethod.GET by defineBooleanResponse {
        val taskId = request.query("taskId")
        controller.downloadManagers.get(taskId)?.status?.state?.let { state ->
          state != DownloadState.Completed && state != DownloadState.Canceled
        } ?: false
      },
    )
    onRenderer {
      controller.renderDownloadWindow(wid)
      getMainWindow().state.setFromManifest(this@DownloadNMM)
    }
  }

  override suspend fun _shutdown() {

  }
}