package org.dweb_browser.microservice.sys.dns

import org.dweb_browser.microservice.help.AdapterManager
import org.dweb_browser.helper.*
import org.dweb_browser.microservice.core.MicroModule
import org.http4k.client.ApacheClient
import org.http4k.core.*

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

val networkFetch =
  ApacheClient(responseBodyMode = BodyMode.Stream, requestBodyMode = BodyMode.Stream)

suspend fun MicroModule.nativeFetch(request: Request): Response {
  for (fetchAdapter in nativeFetchAdaptersManager.adapters) {
    val response = fetchAdapter(this, request)
    if (response != null) {
      return response
    }
  }
  debugFetch("Net/nativeFetch", "$this => ${request.uri}")
  return networkFetch(request)
}

suspend inline fun MicroModule.nativeFetch(url: Uri) = nativeFetch(Request(Method.GET, url))
suspend inline fun MicroModule.nativeFetch(url: String) = nativeFetch(Request(Method.GET, url))
