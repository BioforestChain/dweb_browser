package org.dweb_browser.microservice.http

import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.parameters
import io.ktor.http.parametersOf
import io.ktor.server.util.getOrFail
import org.dweb_browser.helper.Query
import org.dweb_browser.helper.keepFileParameters
import org.dweb_browser.microservice.ipc.helper.IpcHeaders
import org.dweb_browser.microservice.ipc.helper.IpcMethod
import org.dweb_browser.microservice.ipc.helper.IpcRequest

data class PureRequest(
  val url: String,
  val method: IpcMethod,
  val headers: IpcHeaders = IpcHeaders(),
  val body: IPureBody = IPureBody.Empty
) {
  private val parsedUrl by lazy {
    if(url.startsWith("file")) {
      url.keepFileParameters()
    } else {
      Url(url)
    }
  }

  val safeUrl: Url get() = parsedUrl
  fun query(key: String) = parsedUrl.parameters[key]
  fun queryOrFail(key: String) = parsedUrl.parameters.getOrFail(key)
  inline fun <reified T> queryAsObject() = Query.decodeFromUrl<T>(safeUrl)

  companion object {

    fun query(key: String): PureRequest.() -> String? = { query(key) }
    fun <T> query(key: String, transform: String.() -> T): PureRequest.() -> T? =
      { query(key)?.run(transform) }

    fun queryOrFail(key: String): PureRequest.() -> String = { queryOrFail(key) }
    fun <T> queryOrFail(key: String, transform: String.() -> T): PureRequest.() -> T =
      { queryOrFail(key).run(transform) }
  }
}

fun IpcRequest.toPure() = PureRequest(url, method, headers, body.raw)