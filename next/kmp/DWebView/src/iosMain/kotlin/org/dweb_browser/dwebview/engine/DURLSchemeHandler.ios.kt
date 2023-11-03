package org.dweb_browser.dwebview.engine

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.takeFrom
import io.ktor.utils.io.core.toByteArray
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.toCValues
import kotlinx.coroutines.launch
import org.dweb_browser.core.http.IPureBody
import org.dweb_browser.core.http.PureBinaryBody
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureStreamBody
import org.dweb_browser.core.ipc.helper.IpcHeaders
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.http.getFullAuthority
import org.dweb_browser.helper.NSInputStreamToByteReadChannel
import org.dweb_browser.helper.buildUnsafeString
import org.dweb_browser.helper.consumeEachArrayRange
import platform.Foundation.HTTPBodyStream
import platform.Foundation.HTTPMethod
import platform.Foundation.NSData
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSURL
import platform.Foundation.NSURLResponse
import platform.Foundation.allHTTPHeaderFields
import platform.Foundation.create
import platform.WebKit.WKURLSchemeHandlerProtocol
import platform.WebKit.WKURLSchemeTaskProtocol
import platform.WebKit.WKWebView
import platform.darwin.NSInteger
import platform.darwin.NSObject

class DURLSchemeHandler(private val microModule: MicroModule, private val baseUri: Url) :
  NSObject(), WKURLSchemeHandlerProtocol {

  val host get() = baseUri.getFullAuthority()
  val scheme get() = host.replaceFirst(":", "+")

  @Suppress("CONFLICTING_OVERLOADS")
  @OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
  override fun webView(webView: WKWebView, startURLSchemeTask: WKURLSchemeTaskProtocol) {
    try {
      var url = URLBuilder(baseUri).takeFrom(startURLSchemeTask.request.URL!!.resourceSpecifier!!)
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
        url.buildUnsafeString(),
        IpcMethod.valueOf(startURLSchemeTask.request.HTTPMethod!!.uppercase()),
        headers,
        pureBody
      )

      microModule.ioAsyncScope.launch {
        val response = microModule.nativeFetch(pureRequest)

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
                val pointer = StableRef.create(byteArray.toUByteArray().toCValues()).asCPointer()
                startURLSchemeTask.didReceiveData(
                  NSData.create(
                    bytesNoCopy = pointer,
                    length = byteArray.size.toULong(),
                    true
                  )
                )
              }
          }

          is PureBinaryBody -> {
            val byteArray = body.toPureBinary()
            val pointer = StableRef.create(byteArray.toUByteArray().toCValues()).asCPointer()
            startURLSchemeTask.didReceiveData(
              NSData.create(
                bytesNoCopy = pointer,
                length = byteArray.size.toULong(),
                true
              )
            )
          }

          else -> {}
        }

        startURLSchemeTask.didFinish()
      }

    } catch (e: Throwable) {
//      if (e is ObjCErrorException) {
//
//      }

      startURLSchemeTask.didReceiveResponse(
        NSHTTPURLResponse(
          startURLSchemeTask.request.URL ?: NSURL(string = ""),
          502,
          "HTTP/1.1",
          null
        )
      )
      val byteArray = (e.message ?: e.stackTraceToString()).toByteArray()
      val pointer = StableRef.create(byteArray.toUByteArray().toCValues()).asCPointer()
      startURLSchemeTask.didReceiveData(
        NSData.create(
          bytesNoCopy = pointer,
          length = byteArray.size.toULong(),
          true
        )
      )

      startURLSchemeTask.didFinish()
    }
  }

  @Suppress("CONFLICTING_OVERLOADS")
  override fun webView(webView: WKWebView, stopURLSchemeTask: WKURLSchemeTaskProtocol) {
    TODO("Not yet implemented")
  }
}