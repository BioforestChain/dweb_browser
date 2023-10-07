package org.dweb_browser.browser.download

import io.ktor.http.HttpMethod
import kotlinx.serialization.Serializable
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.types.MMID
import org.dweb_browser.microservice.http.PureStream
import org.dweb_browser.microservice.http.bind

class DownloadNMM : NativeMicroModule("download.browser.dweb", "Download") {

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
      "/start" bind HttpMethod.Post to defineStringResponse {
        ""
      },
      // 监控下载进度
      "/progress" bind HttpMethod.Get to definePureStreamHandler {
        PureStream(byteArrayOf())
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
  }

  private fun DownloadTask.pause() {

  }

  override suspend fun _shutdown() {

  }
}