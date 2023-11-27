package org.dweb_browser.dwebview.engine

import io.ktor.utils.io.core.toByteArray
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.pin
import kotlinx.coroutines.Job
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
import platform.Foundation.NSURLRequest
import platform.Foundation.allHTTPHeaderFields
import platform.Foundation.create
import platform.WebKit.WKURLSchemeTaskProtocol
import platform.WebKit.WKWebView
import kotlin.coroutines.cancellation.CancellationException

class DURLSchemeHandlerHelper(private val microModule: MicroModule) {
  private val requestJob = WeakHashMap<NSURLRequest, Job>();

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

    val job = microModule.ioAsyncScope.launch {
      try {

        val response = microModule.nativeFetch(pureRequest)

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
                task.didReceiveData(
                  byteArray.toNSData()
                )
              }
          }

          else -> {
            task.didReceiveData(
              body.toPureBinary().toNSData()
            )
          }
        }

        task.didFinish()
      } catch (e: Throwable) {
        e.printStackTrace()
        task.didReceiveResponse(
          NSHTTPURLResponse(
            taskRequest.URL ?: NSURL(string = ""), 502, "HTTP/1.1", null
          )
        )
        task.didReceiveData(
          NSData.create((e.message ?: e.stackTraceToString()).toByteArray().toNSData())
        )

        task.didFinish()
      }
    }
    requestJob.put(task.request, job)
    job.invokeOnCompletion {
      requestJob.remove(task.request)
    }
  }

  fun stopURLSchemeTask(webView: WKWebView, task: WKURLSchemeTaskProtocol) {
    requestJob.get(task.request)?.apply {
      cancel(CancellationException("stopURLSchemeTask"))
      debugDWebView("stopURLSchemeTask: ${task.request.URL?.absoluteString}")
    }
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