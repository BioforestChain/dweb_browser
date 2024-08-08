package org.dweb_browser.browser.download.ext

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.download.model.DownloadState
import org.dweb_browser.browser.download.model.DownloadStateEvent
import org.dweb_browser.browser.download.model.DownloadTask
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.channelRequest
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.valueNotIn
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureTextFrame
import kotlin.time.Duration.Companion.milliseconds

suspend fun NativeMicroModule.NativeRuntime.createDownloadTask(
  url: String, total: Long? = null, external: Boolean? = null,
): DownloadTask {
  // 将 url 转码，避免 url 内容被解析为 parameter，引起下载地址错误
  val response = nativeFetch(buildUrlString("file://download.browser.dweb/create") {
    parameters["url"] = url
    parameters["total"] = total?.toString() ?: "0L"
    parameters["external"] = external?.toString() ?: "false"
  })
  return response.json<DownloadTask>()
}

suspend fun NativeMicroModule.NativeRuntime.getDownloadTask(taskId: String): DownloadTask? {
  val response = nativeFetch(buildUrlString("file://download.browser.dweb/getTask") {
    parameters["taskId"] = taskId
  })
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
    href = buildUrlString("file://download.browser.dweb/remove") { parameters["taskId"] = taskId },
    method = PureMethod.DELETE
  )
).boolean()

suspend fun NativeMicroModule.NativeRuntime.startDownload(taskId: String) =
  nativeFetch(buildUrlString("file://download.browser.dweb/start") {
    parameters["taskId"] = taskId
  }).boolean()

suspend fun NativeMicroModule.NativeRuntime.pauseDownload(taskId: String) =
  nativeFetch(buildUrlString("file://download.browser.dweb/pause") {
    parameters["taskId"] = taskId
  }).json<DownloadStateEvent>()

suspend fun NativeMicroModule.NativeRuntime.cancelDownload(taskId: String) =
  nativeFetch(buildUrlString("file://download.browser.dweb/cancel") {
    parameters["taskId"] = taskId
  }).boolean()

suspend fun NativeMicroModule.NativeRuntime.downloadProgressFlow(
  taskId: String, fps: Double = 10.0,
) = channelFlow {
  val flowProducer = this
  val throttleMs = (1000.0 / fps).milliseconds
  channelRequest(buildUrlString("file://download.browser.dweb/flow/progress") {
    parameters["taskId"] = taskId
  }) {
    val ctx = this
    ctx.sendText("get")
    for (pureFrame in ctx.income) {
      val now = Clock.System.now()
      val next = now.plus(throttleMs)

      when (pureFrame) {
        is PureTextFrame -> {
          flowProducer.send(Json.decodeFromString<DownloadStateEvent>(pureFrame.text))
          flowProducer.send(null)
          delay(next - Clock.System.now())
          sendText("get")
        }

        else -> {}
      }
    }
    flowProducer.close()
  }
}.buffer(0).filterNotNull()