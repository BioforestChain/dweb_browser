package org.dweb_browser.microservice.http

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.content.MultiPartData
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.util.getOrFail
import io.ktor.util.InternalAPI
import io.ktor.utils.io.copyAndClose
import io.ktor.utils.io.writer
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withContext
import okhttp3.internal.wait
import org.dweb_browser.helper.Query
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.getKtorServerEngine
import org.dweb_browser.helper.platform.httpFetcher
import org.dweb_browser.helper.toIpcUrl
import org.dweb_browser.microservice.ipc.helper.IpcHeaders
import org.dweb_browser.microservice.ipc.helper.IpcMethod
import org.dweb_browser.microservice.ipc.helper.IpcRequest

data class PureRequest(
  val url: String,
  val method: IpcMethod,
  val headers: IpcHeaders = IpcHeaders(),
  val body: IPureBody = IPureBody.Empty,
  val from: Any? = null
) {
  private val parsedUrl by lazy {
    url.toIpcUrl()
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


var midAcc by atomic(0)
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
          }
        }
        call.respond("ok")
      }
    })
  }.let {
    suspend { it.resolvedConnectors().first().port }
  }
}

suspend fun PureRequest.receiveMultipart() = if (from is ApplicationRequest) {
  from.call.receiveMultipart()
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
