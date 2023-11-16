package org.dweb_browser.dwebview.engine

import io.ktor.http.Url
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
import org.dweb_browser.core.std.http.getFullAuthority
import org.dweb_browser.helper.NSInputStreamToByteReadChannel
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.consumeEachArrayRange
import platform.Foundation.HTTPBodyStream
import platform.Foundation.HTTPMethod
import platform.Foundation.NSData
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSURL
import platform.Foundation.allHTTPHeaderFields
import platform.Foundation.create
import platform.WebKit.WKURLSchemeHandlerProtocol
import platform.WebKit.WKURLSchemeTaskProtocol
import platform.WebKit.WKWebView
import platform.darwin.NSObject

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class DURLSchemeHandler(private val microModule: MicroModule, private val baseUri: Url) :
  NSObject(), WKURLSchemeHandlerProtocol {

  internal val host by lazy { baseUri.getFullAuthority() }
  val scheme by lazy { getScheme(host) }

  companion object {
    fun getScheme(host: String) = host.replaceFirst(":", "+")
    fun getScheme(url: Url) = getScheme(url.getFullAuthority())
  }

  @OptIn(BetaInteropApi::class)
  @Suppress("CONFLICTING_OVERLOADS")
  override fun webView(webView: WKWebView, startURLSchemeTask: WKURLSchemeTaskProtocol) {
    try {

      val pureUrl =
        buildUrlString(startURLSchemeTask.request.URL?.absoluteString ?: baseUri.toString()) {
          protocol = baseUri.protocol
          host = baseUri.host
          port = baseUri.port
        }
      println("QAQ startURLSchemeTask: $pureUrl")
      val headers = IpcHeaders()
      startURLSchemeTask.request.allHTTPHeaderFields!!.toMap().map {
        headers.init(it.key as String, it.value as String)
      }

      val pureBody = startURLSchemeTask.request.HTTPBodyStream?.let {
        PureStreamBody(
          NSInputStreamToByteReadChannel(
            microModule.ioAsyncScope,
            it
          )
        )
      } ?: IPureBody.Empty

      val pureRequest = PureRequest(
        pureUrl,
        IpcMethod.valueOf(startURLSchemeTask.request.HTTPMethod!!.uppercase()),
        headers,
        pureBody
      )

      microModule.ioAsyncScope.launch {
        val response = microModule.nativeFetch(pureRequest)
        println("QAQ didReceiveResponse: [${response.status}]$pureUrl")

        startURLSchemeTask.didReceiveResponse(
          NSHTTPURLResponse(
            startURLSchemeTask.request.URL ?: NSURL(string = ""),
            response.status.value.toLong(),
            "HTTP/1.1",
            response.headers.toMap().toMap()
          )
        )
        when (val body = response.body) {
          is PureStreamBody -> {
            body.toPureStream().getReader("DURLSchemeHandler")
              .consumeEachArrayRange { byteArray, _ ->
                startURLSchemeTask.didReceiveData(
                  byteArray.toNSData()
                )
              }
          }

          else -> {
            startURLSchemeTask.didReceiveData(
              body.toPureBinary().toNSData()
            )
          }
        }

        println("QAQ didFinish: [${response.status}]$pureUrl")
        startURLSchemeTask.didFinish()
      }

    } catch (e: Throwable) {
      startURLSchemeTask.didReceiveResponse(
        NSHTTPURLResponse(
          startURLSchemeTask.request.URL ?: NSURL(string = ""),
          502,
          "HTTP/1.1",
          null
        )
      )
      startURLSchemeTask.didReceiveData(
        NSData.create((e.message ?: e.stackTraceToString()).toByteArray().toNSData())
      )

      startURLSchemeTask.didFinish()
    }
  }

  @Suppress("CONFLICTING_OVERLOADS")
  override fun webView(webView: WKWebView, stopURLSchemeTask: WKURLSchemeTaskProtocol) {
    TODO("Not yet implemented webView(webView: WKWebView, stopURLSchemeTask: WKURLSchemeTaskProtocol)")
  }

  @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
  internal fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    val pinned = pin()
    return NSData.create(
      bytesNoCopy = pinned.addressOf(0),
      length = size.toULong(),
      deallocator = { _, _ -> pinned.unpin() }
    )
  }
}