package org.dweb_browser.browser.download

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import okio.Path.Companion.toPath
import org.dweb_browser.browser.MimeTypes
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.core.std.file.commonVirtualFsDirectoryFactory
import org.dweb_browser.core.std.file.fileTypeAdapterManager
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.sys.window.core.onRenderer

internal val debugDownload = Debugger("Download")

class DownloadNMM : NativeMicroModule("download.browser.dweb", "Download") {
  init {
    short_name = "下载管理"
    categories = listOf(
      MICRO_MODULE_CATEGORY.Application,
      MICRO_MODULE_CATEGORY.Network_Service,
      MICRO_MODULE_CATEGORY.Application,
    )
    icons = listOf(ImageResource(src = "file:///sys/icons/$mmid.svg", type = "image/svg+xml"))
    // 初始化下载适配器
    fileTypeAdapterManager.append(
      adapter = commonVirtualFsDirectoryFactory(
        "download",
        getAppContext().dataDir.absolutePath.toPath()
      )
    )
  }

  private val controller = DownloadController(this)

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    /// TODO 从磁盘从恢复出下载信息
    val downloadManagers = mutableMapOf<String, DownloadTask>()
    onAfterShutdown {
      for (task in downloadManagers.values) {
        task.pause()
      }
      downloadManagers.clear()
    }
    routes(
      // 开始下载
      "/start" bind HttpMethod.Post to definePureResponse {
        val mmid = ipc.remote.mmid
        debugDownload("/start", mmid)
        val callbackUrl = request.queryOrNull("callbackUrl")
        val originUrl = request.queryOrNull("originUrl")
        val url = request.query("url")

        val task = controller.downloadTaskMap.getOrPut(mmid) {
          DownloadTask(
            id = mmid,
            url = url,
            status = DownloadProgressEvent(
              state = DownloadState.Init,
            ),
            createTime = System.currentTimeMillis(),
            originMmid = mmid,
            originUrl = originUrl,
            completeCallbackUrl = callbackUrl,
            mime = MimeTypes.getMimeTypeFromExtension(url)
          )
        }
        if (controller.downloadFactory(task)) {
          return@definePureResponse PureResponse(HttpStatusCode.OK)
        } else {
          return@definePureResponse PureResponse(HttpStatusCode.ExpectationFailed).body("Download Process exists ${ipc.remote.mmid}")
        }
      },
      // 监控下载进度
      "/watch/progress" bind HttpMethod.Get to defineJsonLineResponse {
        controller.onDownload { info ->
          emit(info.status)
        }
      },
      // 暂停下载
      "/pause" bind HttpMethod.Put to defineBooleanResponse {
        true
      },
      // 恢复下载
      "/resume" bind HttpMethod.Put to defineBooleanResponse {
        true
      },
      // 取消下载
      "/cancel" bind HttpMethod.Put to defineBooleanResponse {
        true
      },
    )
    onRenderer {

    }
  }

  private fun DownloadTask.pause() {

  }


  override suspend fun _shutdown() {

  }
}