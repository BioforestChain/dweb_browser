package org.dweb_browser.microservice.sys.dns

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.request.prepareRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.printdebugln
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.help.AdapterManager
import org.dweb_browser.microservice.help.toHttpRequestBuilder
import org.dweb_browser.microservice.help.toResponse
import org.dweb_browser.microservice.ipc.helper.ReadableStreamOut
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri


typealias FetchAdapter = suspend (remote: MicroModule, request: Request) -> Response?

fun debugFetch(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("fetch", tag, msg, err)

fun debugFetchFile(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("fetch-file", tag, msg, err)

/**
 * file:/// => /usr & /sys as const
 * file://file.sys.dweb/ => /home & /tmp & /share as userData
 */
val nativeFetchAdaptersManager = NativeFetchAdaptersManager()

class NativeFetchAdaptersManager : AdapterManager<FetchAdapter>() {

  private var client = HttpClient(CIO) {
    install(HttpCache)
  }

  fun setClientProvider(client: HttpClient) {
    this.client = client
  }

  suspend fun httpFetch(request: Request) = try {
    debugFetch("httpFetch request", request.uri)
    val responsePo = PromiseOut<Response>()
    CoroutineScope(ioAsyncExceptionHandler).launch {
      client.prepareRequest(request.toHttpRequestBuilder()).execute {
        val streamOut = ReadableStreamOut();
        val response = it.toResponse(streamOut)
        debugFetch("httpFetch response", request.uri)
        responsePo.resolve(response)
        streamOut.stream.waitClosed()
        debugFetch("httpFetch end", request.uri)
      }
    }
    responsePo.waitPromise().also {
      debugFetch("httpFetch return", request.uri)
    }
  } catch (e: Throwable) {
    Response(Status.SERVICE_UNAVAILABLE).body(e.stackTraceToString())
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
