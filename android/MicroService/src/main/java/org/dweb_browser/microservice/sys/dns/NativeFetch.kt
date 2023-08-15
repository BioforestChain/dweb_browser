package org.dweb_browser.microservice.sys.dns

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.request
import org.dweb_browser.helper.printdebugln
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.help.AdapterManager
import org.dweb_browser.microservice.help.toHttpRequestBuilder
import org.dweb_browser.microservice.help.toResponse
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
val nativeFetchAdaptersManager = AdapterManager<FetchAdapter>()

private val client = HttpClient(CIO)
suspend fun httpFetch(request: Request) = try {
  client.request(request.toHttpRequestBuilder()).toResponse()
} catch (e: Throwable) {
  Response(Status.SERVICE_UNAVAILABLE).body(e.stackTraceToString())
}

suspend fun MicroModule.nativeFetch(request: Request): Response {
  for (fetchAdapter in nativeFetchAdaptersManager.adapters) {
    val response = fetchAdapter(this, request)
    if (response != null) {
      return response
    }
  }
  debugFetch("Net/nativeFetch", "$this => ${request.uri}")

  return httpFetch(request)
}


suspend inline fun MicroModule.nativeFetch(url: Uri) = nativeFetch(Request(Method.GET, url))
suspend inline fun MicroModule.nativeFetch(url: String) = nativeFetch(Request(Method.GET, url))
