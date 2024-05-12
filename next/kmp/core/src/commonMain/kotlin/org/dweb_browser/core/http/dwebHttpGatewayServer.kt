package org.dweb_browser.core.http


import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.help.AdapterManager
import org.dweb_browser.core.http.router.ResponseException
import org.dweb_browser.core.std.http.debugHttp
import org.dweb_browser.helper.DeferredSignal
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.pure.http.HttpPureServer
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureServerRequest


typealias HttpGateway = suspend (request: PureServerRequest) -> PureResponse?

class DwebGatewayHandlerAdapterManager : AdapterManager<HttpGateway>() {
  suspend fun doGateway(request: PureServerRequest): PureResponse? {
    try {
      for (adapter in adapters) {
        val response = adapter(request)
        if (response != null) {
          return response
        }
      }
      return null
    } catch (e: ResponseException) {
      return PureResponse(
        HttpStatusCode(e.code.value, e.message),
        body = IPureBody.from(e.cause?.message ?: "")
      )
    } catch (e: Throwable) {
      return PureResponse(
        HttpStatusCode.InternalServerError,
        body = IPureBody.from(e.message ?: "")
      )
    }
  }
}

class DwebHttpGatewayServer private constructor() {
  companion object {
    val INSTANCE by lazy { DwebHttpGatewayServer() }
    val gatewayAdapterManager = DwebGatewayHandlerAdapterManager()
  }

  val server = HttpPureServer { rawRequest ->
    val pureRequest = when (val url = rawRequest.queryOrNull("X-Dweb-Url")) {
      null -> rawRequest
      else -> rawRequest.copy(href = url)
    };
    debugHttp("doGateway", pureRequest.href)
    gatewayAdapterManager.doGateway(pureRequest)
  }

  val startServer = SuspendOnce {
    debugHttp("DwebHttpGatewayServer", "start")
    if (closedDeferred.isCompleted) {
      closedDeferred = CompletableDeferred<Unit>()
    }
    server.start(0u).toInt()
  }

  suspend fun closeServer() {
    debugHttp("DwebHttpGatewayServer", "close")
    server.close()
    closedDeferred.complete(Unit)
    startServer.reset()// 服务已经关闭，可以重启
  }

  private var closedDeferred = CompletableDeferred<Unit>()
  val onClosed = DeferredSignal(closedDeferred)

  val getHttpLocalhostGatewaySuffix = SuspendOnce { ".localhost:${startServer()}" }

  suspend fun getUrl() = "http://127.0.0.1:${startServer()}"

}

val dwebHttpGatewayServer get() = DwebHttpGatewayServer.INSTANCE