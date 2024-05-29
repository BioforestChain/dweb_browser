package org.dweb_browser.browser.download.ext

import io.ktor.util.encodeBase64
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.DownloadStateEvent
import org.dweb_browser.browser.download.DownloadTask
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.createChannel
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.valueNotIn
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureTextFrame
import kotlin.time.Duration.Companion.microseconds

suspend fun NativeMicroModule.NativeRuntime.createDownloadTask(
  url: String, total: Long? = null, external: Boolean? = null,
): DownloadTask {
  // 将 url 转码，避免 url 内容被解析为 parameter，引起下载地址错误
  val encodeUrl = url.encodeToByteArray().encodeBase64()
  val response = nativeFetch(
    url = "file://download.browser.dweb/create?url=$encodeUrl&total=${total ?: 0L}&external=${external ?: false}"
  )
  return response.json<DownloadTask>()
}

suspend fun NativeMicroModule.NativeRuntime.getDownloadTask(taskId: String): DownloadTask? {
  val response = nativeFetch("file://download.browser.dweb/getTask?taskId=$taskId")
  return if (response.isOk) {
    response.json<DownloadTask>()
  } else null
}

suspend fun NativeMicroModule.NativeRuntime.existDownloadTask(taskId: String): Boolean {
  return getDownloadTask(taskId)?.status?.state?.valueNotIn(
    DownloadState.Completed, DownloadState.Canceled
  ) ?: false
}

suspend fun NativeMicroModule.NativeRuntime.removeDownload(taskId: String) = nativeFetch(
  PureClientRequest(
    href = "file://download.browser.dweb/remove?taskId=${taskId}", method = PureMethod.DELETE
  )
).boolean()

suspend fun NativeMicroModule.NativeRuntime.startDownload(taskId: String) =
  nativeFetch("file://download.browser.dweb/start?taskId=$taskId").boolean()

suspend fun NativeMicroModule.NativeRuntime.pauseDownload(taskId: String) =
  nativeFetch("file://download.browser.dweb/pause?taskId=$taskId").json<DownloadStateEvent>()

suspend fun NativeMicroModule.NativeRuntime.cancelDownload(taskId: String) =
  nativeFetch("file://download.browser.dweb/cancel?taskId=$taskId").boolean()

suspend fun NativeMicroModule.NativeRuntime.downloadProgressFlow(
  taskId: String, fps: Double = 10.0,
) = channelFlow {
  val channelFlow = this
  val throttleMs = (1000.0 / fps).microseconds
  createChannel("file://download.browser.dweb/flow/progress?taskId=$taskId") {
    val ctx = this
    ctx.sendText("get")
    for (pureFrame in ctx.income) {
      val now = Clock.System.now()
      val next = now.plus(throttleMs)

      when (pureFrame) {
        is PureTextFrame -> {
          channelFlow.send(Json.decodeFromString<DownloadStateEvent>(pureFrame.text))
          channelFlow.send(null)
          delay(next - Clock.System.now())
          sendText("get")
        }

        else -> {}
      }
    }
    channelFlow.close()
  }
}.filterNotNull()