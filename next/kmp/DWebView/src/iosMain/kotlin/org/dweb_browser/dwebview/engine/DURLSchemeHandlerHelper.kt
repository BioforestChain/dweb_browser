package org.dweb_browser.dwebview.engine

import io.ktor.utils.io.core.toByteArray
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ForeignException
import kotlinx.coroutines.launch
import org.dweb_browser.core.http.IPureBody
import org.dweb_browser.core.http.PureClientRequest
import org.dweb_browser.core.http.PureStreamBody
import org.dweb_browser.core.ipc.helper.IpcHeaders
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.NSInputStreamToByteReadChannel
import org.dweb_browser.helper.consumeEachArrayRange
import org.dweb_browser.helper.platform.NSDataHelper.toNSData
import org.dweb_browser.helper.platform.ios.URLSchemeTaskHelper
import platform.Foundation.HTTPBodyStream
import platform.Foundation.HTTPMethod
import platform.Foundation.NSData
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSURL
import platform.Foundation.allHTTPHeaderFields
import platform.Foundation.create
import platform.WebKit.WKURLSchemeTaskProtocol
import platform.WebKit.WKWebView

@OptIn(ExperimentalForeignApi::class)
class DURLSchemeHandlerHelper(private val microModule: MicroModule) {
  private val nativeHelper = URLSchemeTaskHelper()

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

    val pureRequest = PureClientRequest(
      pureUrl, IpcMethod.valueOf(taskRequest.HTTPMethod!!.uppercase()), headers, pureBody
    )

    val easyTask = nativeHelper.startURLSchemeTask(
      webView = webView,
      task = task
    )

    microModule.ioAsyncScope.launch {
      try {

        val response = microModule.nativeFetch(pureRequest)

        easyTask.didReceiveResponse(
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
                easyTask.didReceiveData(
                  byteArray.toNSData()
                )
              }
          }

          else -> {
            easyTask.didReceiveData(
              body.toPureBinary().toNSData()
            )
          }
        }

        easyTask.didFinish()
      } catch (e: RuntimeException) {
        println("RuntimeException: ${e.message}")
      } catch (e: ForeignException) {
        println("ForeignException: ${e.message}")
      } catch (e: Throwable) {
        e.printStackTrace()
        easyTask.didReceiveResponse(
          NSHTTPURLResponse(
            taskRequest.URL ?: NSURL(string = ""), 502, "HTTP/1.1", null
          )
        )
        easyTask.didReceiveData(
          NSData.create((e.message ?: e.stackTraceToString()).toByteArray().toNSData())
        )

        easyTask.didFinish()
      }
    }
  }

  fun stopURLSchemeTask(webView: WKWebView, task: WKURLSchemeTaskProtocol) {
    nativeHelper.stopURLSchemeTask(
      webView = webView,
      task = task
    )
  }
}
