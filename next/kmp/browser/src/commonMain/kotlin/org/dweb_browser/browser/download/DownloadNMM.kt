package org.dweb_browser.browser.download

import io.ktor.http.HttpMethod
import kotlinx.serialization.Serializable
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.bind
import org.dweb_browser.core.std.file.commonVirtualFsDirectoryFactory
import org.dweb_browser.sys.window.core.onRenderer

class DownloadNMM : NativeMicroModule("download.browser.dweb", "Download") {
  init {
    short_name = "Download"
    categories = listOf(
      MICRO_MODULE_CATEGORY.Network_Service,
      MICRO_MODULE_CATEGORY.Application,
    )
    icons = listOf(ImageResource(src = "file:///sys/icons/$mmid.svg", type = "image/svg+xml"))
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    /// TODO 从磁盘从恢复出下载信息
    val downloadManagers = mutableMapOf<String, DownloadTask>()
    onAfterShutdown {
      for (task in downloadManagers.values) {
        task.pause()
      }
      downloadManagers.clear()
    }
//    fileTypeAdapterManager.append(
//      adapter = commonVirtualFsDirectoryFactory(
//        "download",
//        "/data/download"
//      )
//    )
    routes(
      // 开始下载
      "/start" bind HttpMethod.Post to defineStringResponse {
        ""
      },
      // 监控下载进度
      "/watch/progress" bind HttpMethod.Get to defineJsonLineResponse {
        @Serializable
        data class Progress(val current: Long, val total: Long)
        emit(Progress(0, 1))
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