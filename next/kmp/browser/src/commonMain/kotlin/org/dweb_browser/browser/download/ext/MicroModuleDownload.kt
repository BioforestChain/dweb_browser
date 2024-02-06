package org.dweb_browser.browser.download.ext

import kotlinx.serialization.json.Json
import org.dweb_browser.browser.download.DownloadTask
import org.dweb_browser.browser.download.debugDownload
import org.dweb_browser.browser.jmm.debugJMM
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.createChannel
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.pure.http.PureChannelContext
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureTextFrame
import kotlin.coroutines.cancellation.CancellationException

suspend fun NativeMicroModule.createDownloadTask(
  url: String, total: Long = 1L, external: Boolean = false
) = nativeFetch(
  url = "file://download.browser.dweb/create?url=$url&total=$total&external=$external"
).text()

suspend fun NativeMicroModule.startDownload(taskId: String) =
  nativeFetch("file://download.browser.dweb/start?taskId=$taskId").boolean()

suspend fun NativeMicroModule.pauseDownload(taskId: String) =
  nativeFetch("file://download.browser.dweb/pause?taskId=$taskId").boolean()

suspend fun NativeMicroModule.cancelDownload(taskId: String) =
  nativeFetch("file://download.browser.dweb/cancel?taskId=$taskId").boolean()

suspend fun NativeMicroModule.existsDownload(taskId: String) =
  nativeFetch("file://download.browser.dweb/exists?taskId=$taskId").boolean()

suspend fun NativeMicroModule.currentDownload(taskId: String) =
  nativeFetch("file://download.browser.dweb/current?taskId=$taskId").long()

suspend fun NativeMicroModule.removeDownload(taskId: String) = nativeFetch(
  PureClientRequest(
    href = "file://download.browser.dweb/remove?taskId=${taskId}", method = PureMethod.DELETE
  )
).boolean()

suspend fun NativeMicroModule.createChannelOfDownload(
  taskId: String, resolve: suspend WatchDownloadContext.() -> Unit,
) = createChannel("file://download.browser.dweb/watch/progress?taskId=$taskId"){
  for (pureFrame in income){
    when (pureFrame) {
      is PureTextFrame -> {
        WatchDownloadContext(Json.decodeFromString<DownloadTask>(pureFrame.data),this).resolve()
      }
      else -> {}
    }
  }
}

class WatchDownloadContext(val downloadTask: DownloadTask,val channel:PureChannelContext)
