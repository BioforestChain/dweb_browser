package org.dweb_browser.core.http

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.content.MultiPartData
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.util.getOrFail
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.ipc.helper.IpcHeaders
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.ipc.helper.IpcRequest
import org.dweb_browser.helper.Query
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.getKtorServerEngine
import org.dweb_browser.helper.platform.httpFetcher
import org.dweb_browser.helper.toIpcUrl

data class PureRequest(
  val href: String,
  val method: IpcMethod,
  val headers: IpcHeaders = IpcHeaders(),
  val body: IPureBody = IPureBody.Empty,
  val origin: Any? = null
) {
  val url by lazy {
    href.toIpcUrl()
  }

  fun queryOrNull(key: String) = this.url.parameters[key]
  fun query(key: String) = this.url.parameters.getOrFail(key)
  inline fun <reified T> queryAs() = Query.decodeFromUrl<T>(this.url)
  inline fun <reified T> queryAs(key: String) = Json.decodeFromString<T>(query(key))
  inline fun <reified T> queryAsOrNull(key: String) =
    queryOrNull(key)?.let { Json.decodeFromString<T>(it) }

  companion object {

    fun queryOrNull(key: String): PureRequest.() -> String? = { queryOrNull(key) }
    fun <T> queryOrNull(key: String, transform: String.() -> T): PureRequest.() -> T? =
      { queryOrNull(key)?.run(transform) }

    fun query(key: String): PureRequest.() -> String = { query(key) }
    fun <T> query(key: String, transform: String.() -> T): PureRequest.() -> T =
      { query(key).run(transform) }

    inline fun <reified T> fromJson(
      href: String,
      method: IpcMethod,
      body: T,
      headers: IpcHeaders = IpcHeaders(),
      origin: Any? = null
    ) = PureRequest(
      href, method, headers.apply { init("Content-Type", "application/json") }, IPureBody.from(
        Json.encodeToString(body)
      ), origin = origin
    )
  }
}

fun IpcRequest.toPure() = PureRequest(url, method, headers, body.raw)


var midAcc by SafeInt(0)
val multipartProxyResponseMap by lazy {
  mutableMapOf<Int, CompletableDeferred<MultiPartData>>()
}
val multipartProxyServer by lazy {
  embeddedServer(getKtorServerEngine(), port = 0) {
    install(createApplicationPlugin("multipartProxy") {
      onCall { call ->
        withContext(ioAsyncExceptionHandler) {
          val mid = (call.request.queryParameters["mid"] ?: return@withContext).toInt()
          val deferred = multipartProxyResponseMap.remove(mid) ?: return@withContext
          try {
            deferred.complete(call.receiveMultipart())
          } catch (e: Throwable) {
            deferred.completeExceptionally(e)
          } finally {
            call.respond("ok")
          }
        }
      }
    })
  }.start(wait = false).let {
    suspend {
      it.resolvedConnectors().first().port
    }
  }
}

suspend fun PureRequest.receiveMultipart() = if (origin is ApplicationRequest) {
  origin.call.receiveMultipart()
} else {
  val pureRequest = this
  val mid = midAcc++
  val deferred = CompletableDeferred<MultiPartData>();
  multipartProxyResponseMap[mid] = deferred
  httpFetcher.post("http://localhost:${multipartProxyServer()}/?mid=$mid") {
    for ((key, value) in pureRequest.headers.toHttpHeaders()) {
      header(key, value)
    }
    setBody(pureRequest.body.toPureStream().getReader("receiveMultipart"))
  }
  deferred.await()
}
