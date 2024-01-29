package org.dweb_browser.browser.download.ext

import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.createChannel
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureFrame
import org.dweb_browser.pure.http.PureMethod

suspend fun NativeMicroModule.createDownloadTask(url: String, total: Long) =
  nativeFetch("file://download.browser.dweb/create?url=$url&total=$total").text()

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

suspend fun NativeMicroModule.removeDownload(taskId: String) =
  nativeFetch(
    PureClientRequest(
      "file://download.browser.dweb/remove?taskId=${taskId}", PureMethod.DELETE
    )
  ).boolean()

suspend fun NativeMicroModule.createChannelOfDownload(
  taskId: String, resolve: suspend (frame: PureFrame, close: (suspend () -> Unit)) -> Unit,
) = createChannel("file://download.browser.dweb/watch/progress?taskId=$taskId", resolve)

