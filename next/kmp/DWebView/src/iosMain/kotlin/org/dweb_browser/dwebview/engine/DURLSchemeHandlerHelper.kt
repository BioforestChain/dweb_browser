package org.dweb_browser.dwebview.engine

import io.ktor.utils.io.core.toByteArray
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.pin
import kotlinx.coroutines.launch
import org.dweb_browser.core.http.IPureBody
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureStreamBody
import org.dweb_browser.core.ipc.helper.IpcHeaders
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.helper.NSInputStreamToByteReadChannel
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.consumeEachArrayRange
import platform.Foundation.HTTPBodyStream
import platform.Foundation.HTTPMethod
import platform.Foundation.NSData
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSURL
import platform.Foundation.allHTTPHeaderFields
import platform.Foundation.create
import platform.WebKit.WKURLSchemeTaskProtocol
import platform.WebKit.WKWebView

class DURLSchemeHandlerHelper(private val microModule: MicroModule) {
  private val stopped = WeakHashMap<WKURLSchemeTaskProtocol, WKWebView>();

  @OptIn(BetaInteropApi::class)
  fun startURLSchemeTask(webView: WKWebView, task: WKURLSchemeTaskProtocol, pureUrl: String) {
    val taskRequest = task.request

    val headers = IpcHeaders()
    taskRequest.allHTTPHeaderFields!!.toMap().map {
      headers.init(it.key as String, it.value as String)
    }

    val pureBody = taskRequest.HTTPBodyStream?.let {
      PureStreamBody(
        NSInputStreamToByteReadChannel(
          microModule.ioAsyncScope, it
        )
      )
    } ?: IPureBody.Empty

    val pureRequest = PureRequest(
      pureUrl,
      IpcMethod.valueOf(taskRequest.HTTPMethod!!.uppercase()),
      headers,
      pureBody
    )

    microModule.ioAsyncScope.launch {
      try {

        val response = microModule.nativeFetch(pureRequest)

        if (stopped.containsKey(task)) return@launch
        task.didReceiveResponse(
          NSHTTPURLResponse(
            taskRequest.URL ?: NSURL(string = ""),
            response.status.value.toLong(),
            "HTTP/1.1",
            response.headers.toMap().toMap()
          )
        )
        when (val body = response.body) {
          is PureStreamBody -> {
            body.toPureStream().getReader("DURLSchemeHandler")
              .consumeEachArrayRange { byteArray, _ ->
                if (stopped.containsKey(task)) return@launch
                task.didReceiveData(
                  byteArray.toNSData()
                )
              }
          }

          else -> {
            if (stopped.containsKey(task)) return@launch
            task.didReceiveData(
              body.toPureBinary().toNSData()
            )
          }
        }

        if (stopped.containsKey(task)) return@launch
        task.didFinish()
      } catch (e: Throwable) {
        catchFinishURLSchemeTask(e, webView, task, pureUrl)
      }
    }

  }

  @OptIn(BetaInteropApi::class)
  private fun catchFinishURLSchemeTask(
    e: Throwable,
    webView: WKWebView,
    task: WKURLSchemeTaskProtocol,
    pureUrl: String
  ) {
    println("QAQ!!!")
    e.printStackTrace()
    if (stopped.containsKey(task)) return
    val taskRequest = task.request
    try {
      task.didReceiveResponse(
        NSHTTPURLResponse(
          taskRequest.URL ?: NSURL(string = ""), 502, "HTTP/1.1", null
        )
      )
      task.didReceiveData(
        NSData.create((e.message ?: e.stackTraceToString()).toByteArray().toNSData())
      )

      task.didFinish()
    } catch (_: Throwable) {
    }
  }

  fun stopURLSchemeTask(webView: WKWebView, task: WKURLSchemeTaskProtocol) {
    debugDWebView("stopURLSchemeTask: ${task.request.URL?.absoluteString}")
    stopped.put(task, webView)
    /// 这里不能对task做操作，它已经被stop了，所以只能做一些数据读取，处理自己的事务
  }

  @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
  internal fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    val pinned = pin()
    return NSData.create(bytesNoCopy = pinned.addressOf(0),
      length = size.toULong(),
      deallocator = { _, _ -> pinned.unpin() })
  }
}