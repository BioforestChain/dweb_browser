package org.dweb_browser.microservice.sys.dns

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpMethod
import io.ktor.utils.io.jvm.javaio.toInputStream
import org.dweb_browser.helper.printdebugln
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.help.AdapterManager
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

val client = HttpClient(CIO)

suspend fun MicroModule.nativeFetch(request: Request): Response {
  for (fetchAdapter in nativeFetchAdaptersManager.adapters) {
    val response = fetchAdapter(this, request)
    if (response != null) {
      return response
    }
  }
  debugFetch("Net/nativeFetch", "$this => ${request.uri}")

  return client.request(HttpRequestBuilder().also { httpRequestBuilder ->
    httpRequestBuilder.method = HttpMethod.parse(request.method.name)
    for ((key, value) in request.headers) {
      httpRequestBuilder.header(key, value)
    }
    httpRequestBuilder.setBody(request.body.stream)
  }).let { httpResponse ->
    Response(
      Status(httpResponse.status.value, httpResponse.status.description),
      httpResponse.version.toString()
    ).headers(mutableListOf<Pair<String, String>>().also { headers ->
      httpResponse.headers.forEach { key, values ->
        for (value in values) {
          headers.add(Pair(key, value))
        }
      }
    }).body(httpResponse.bodyAsChannel().toInputStream())
  }
}

suspend inline fun MicroModule.nativeFetch(url: Uri) = nativeFetch(Request(Method.GET, url))
suspend inline fun MicroModule.nativeFetch(url: String) = nativeFetch(Request(Method.GET, url))
