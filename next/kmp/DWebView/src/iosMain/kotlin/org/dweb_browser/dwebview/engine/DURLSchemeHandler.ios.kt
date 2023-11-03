package org.dweb_browser.dwebview.engine

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.takeFrom
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
          NSHttpResponseFactory(
            startURLSchemeTask.request.URL,
            response.status.value.toLong(),
            response.headers
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
        NSHttpResponseFactory(
          startURLSchemeTask.request.URL,
          502L, IpcHeaders()
        )
      )
      NSData.create(e.message ?: e.stackTraceToString())
        ?.let { startURLSchemeTask.didReceiveData(it) }
      startURLSchemeTask.didFinish()
    }
  }

//  override fun webView(webView: WKWebView, stopURLSchemeTask: WKURLSchemeTaskProtocol) {
//    TODO("Not yet implemented")
//  }

  internal class NSHttpResponseFactory(
    private val url: NSURL?,
    private val code: Long,
    private val headers: IpcHeaders
  ) : NSHTTPURLResponse() {
    override fun statusCode(): NSInteger = code
    override fun URL(): NSURL? = url

    override fun allHeaderFields(): Map<Any?, *> = headers.toMap().toMap()
  }
}