package org.dweb_browser.dwebview.engine

import io.ktor.utils.io.core.toByteArray
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ForeignException
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.NSInputStreamToByteReadChannel
import org.dweb_browser.helper.consumeEachArrayRange
import org.dweb_browser.helper.platform.NSDataHelper.toNSData
import org.dweb_browser.platform.ios.URLSchemeTaskHelper
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureStreamBody
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
class DURLSchemeHandlerHelper(private val microModule: MicroModule.Runtime) {
  private val nativeHelper = URLSchemeTaskHelper()

  @OptIn(BetaInteropApi::class)
  fun startURLSchemeTask(webView: WKWebView, task: WKURLSchemeTaskProtocol, pureUrl: String) {
    val taskRequest = task.request

    val headers = PureHeaders()
    taskRequest.allHTTPHeaderFields!!.toMap().map {
      headers.init(it.key as String, it.value as String)
    }

    val pureBody = taskRequest.HTTPBodyStream?.let {
      PureStreamBody(
        NSInputStreamToByteReadChannel(
          microModule.getRuntimeScope(), it
        )
      )
    } ?: IPureBody.Empty

    val pureRequest = PureClientRequest(
      pureUrl, PureMethod.ALL_VALUES[taskRequest.HTTPMethod!!.uppercase()]?:throw Exception("HTTPMethod:${taskRequest.HTTPMethod} is invalid PureMethod"), headers, pureBody
    )

    val easyTask = nativeHelper.startURLSchemeTask(
      webView = webView,
      task = task
    )

    microModule.scopeLaunch(cancelable = true) {
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
