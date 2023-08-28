package org.dweb_browser.microservice.sys.dns

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.prepareRequest
import io.ktor.util.toLowerCasePreservingASCIIRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.printDebug
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.help.AdapterManager
import org.dweb_browser.microservice.help.toHttpRequestBuilder
import org.dweb_browser.microservice.help.toResponse
import org.dweb_browser.microservice.ipc.helper.ReadableStreamOut
import org.http4k.base64DecodedArray
import org.http4k.core.MemoryBody
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.routing.path

typealias FetchAdapter = suspend (remote: MicroModule, request: Request) -> Response?

fun debugFetch(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("fetch", tag, msg, err)

fun debugFetchFile(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("fetch-file", tag, msg, err)

/**
 * file:/// => /usr & /sys as const
 * file://file.sys.dweb/ => /home & /tmp & /share as userData
 */
val nativeFetchAdaptersManager = NativeFetchAdaptersManager()

class NativeFetchAdaptersManager : AdapterManager<FetchAdapter>() {

  private var client = HttpClient(CIO) {
    //install(HttpCache)
  }

  fun setClientProvider(client: HttpClient) {
    this.client = client
  }

  suspend fun httpFetch(request: Request): Response {
    try {
      debugFetch("httpFetch request", request.uri)

      // if (request.uri.scheme == "data") {
      //   val dataUriContent = request.uri.path
      //   val dataUriContentInfo = dataUriContent.split(',', limit = 2)
      //   when (dataUriContentInfo.size) {
      //     2 -> {
      //       val meta = dataUriContentInfo[0]
      //       val bodyContent = dataUriContentInfo[1]
      //       val metaInfo = meta.split(';', limit = 2)
      //       val response = Response(Status.OK)
      //       when (metaInfo.size) {
      //         1 -> {
      //           return response.body(bodyContent).header("Content-Type", meta)
      //         }

      //         2 -> {
      //           val encoding = metaInfo[1]
      //           return if (encoding.trim().toLowerCasePreservingASCIIRules() == "base64") {
      //             response.header("Content-Type", metaInfo[0]).body(MemoryBody(bodyContent.base64DecodedArray()))
      //           } else {
      //             response.header("Content-Type", meta).body(bodyContent)
      //           }
      //         }
      //       }
      //     }
      //   }
      //   /// 保底操作
      //   return Response(Status.OK).body(dataUriContent)
      // }
      val responsePo = PromiseOut<Response>()
      CoroutineScope(ioAsyncExceptionHandler).launch {
        client.prepareRequest(request.toHttpRequestBuilder()).execute {
          val streamOut = ReadableStreamOut()
          val response = it.toResponse(streamOut)
          debugFetch("httpFetch response", request.uri)
          responsePo.resolve(response)
          streamOut.stream.waitClosed()
          debugFetch("httpFetch end", request.uri)
        }
      }
      return responsePo.waitPromise().also {
        debugFetch("httpFetch return", request.uri)
      }
    } catch (e: Throwable) {
      return Response(Status.SERVICE_UNAVAILABLE).body(e.stackTraceToString())
    }
  }
}

val httpFetch = nativeFetchAdaptersManager::httpFetch

suspend fun MicroModule.nativeFetch(request: Request): Response {
  for (fetchAdapter in nativeFetchAdaptersManager.adapters) {
    val response = fetchAdapter(this, request)
    if (response != null) {
      return response
    }
  }

  return nativeFetchAdaptersManager.httpFetch(request)
}

suspend inline fun MicroModule.nativeFetch(url: Uri) = nativeFetch(Request(Method.GET, url))
suspend inline fun MicroModule.nativeFetch(url: String) = nativeFetch(Request(Method.GET, url))
