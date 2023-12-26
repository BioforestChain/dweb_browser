package org.dweb_browser.pure.http

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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withContext
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.pure.http.engine.getKtorServerEngine
import org.dweb_browser.pure.http.engine.httpFetcher

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

suspend fun PureServerRequest.receiveMultipart() = findFrom {
  if (it is ApplicationRequest) {
    it
  } else null
}.let { appRequest ->
  if (appRequest != null) {
    appRequest.call.receiveMultipart()
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
}