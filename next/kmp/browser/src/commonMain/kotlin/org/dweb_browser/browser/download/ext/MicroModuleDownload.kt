package org.dweb_browser.browser.download.ext

import io.ktor.util.encodeBase64
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.download.DownloadStateEvent
import org.dweb_browser.browser.download.DownloadTask
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.createChannel
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.pure.http.PureChannelContext
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureTextFrame

suspend fun NativeMicroModule.NativeRuntime.createDownloadTask(
  url: String, total: Long? = null, external: Boolean? = null
): DownloadTask {
  // 将 url 转码，避免 url 内容被解析为 parameter，引起下载地址错误
  val encodeUrl = url.encodeToByteArray().encodeBase64()
  val response = nativeFetch(
    url = "file://download.browser.dweb/create?url=$encodeUrl&total=${total ?: 0L}&external=${external ?: false}"
  )
  return response.json<DownloadTask>()
}

suspend fun NativeMicroModule.NativeRuntime.startDownload(taskId: String) =
  nativeFetch("file://download.browser.dweb/start?taskId=$taskId").boolean()

suspend fun NativeMicroModule.NativeRuntime.pauseDownload(taskId: String) =
  nativeFetch("file://download.browser.dweb/pause?taskId=$taskId").boolean()

suspend fun NativeMicroModule.NativeRuntime.cancelDownload(taskId: String) =
  nativeFetch("file://download.browser.dweb/cancel?taskId=$taskId").boolean()

suspend fun NativeMicroModule.NativeRuntime.existsDownload(taskId: String) =
  nativeFetch("file://download.browser.dweb/exists?taskId=$taskId").boolean()

suspend fun NativeMicroModule.NativeRuntime.currentDownload(taskId: String) =
  nativeFetch("file://download.browser.dweb/current?taskId=$taskId").long()

suspend fun NativeMicroModule.NativeRuntime.removeDownload(taskId: String) = nativeFetch(
  PureClientRequest(
    href = "file://download.browser.dweb/remove?taskId=${taskId}", method = PureMethod.DELETE
  )
).boolean()

suspend fun NativeMicroModule.NativeRuntime.watchDownloadProgress(
  taskId: String, resolve: suspend WatchDownloadContext.() -> Unit,
) = createChannel("file://download.browser.dweb/watch/progress?taskId=$taskId") {
  for (pureFrame in income) {
    when (pureFrame) {
      is PureTextFrame -> {
        WatchDownloadContext(Json.decodeFromString<DownloadStateEvent>(pureFrame.text), this).resolve()
      }

      else -> {}
    }
  }
}

class WatchDownloadContext(val status: DownloadStateEvent, val channel: PureChannelContext)
