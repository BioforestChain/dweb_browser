package org.dweb_browser.core.http


import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.help.AdapterManager
import org.dweb_browser.core.std.http.debugHttp
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.pure.http.HttpPureServer
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureServerRequest


typealias HttpGateway = suspend (request: PureServerRequest) -> PureResponse?

class DwebGatewayHandlerAdapterManager : AdapterManager<HttpGateway>() {
  suspend fun doGateway(request: PureServerRequest): PureResponse? {
    for (adapter in adapters) {
      val response = adapter(request)
      if (response != null) {
        return response
      }
    }
    return null
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
    try {
      gatewayAdapterManager.doGateway(pureRequest)
    } catch (e: Throwable) {
      PureResponse(HttpStatusCode.BadGateway, body = IPureBody.from(e.message ?: ""))
    }
  }

  val startServer = SuspendOnce {
    server.start(0u).toInt()
  }

  val closeServer = SuspendOnce {
    server.close()
  }

  suspend fun getPort() = startServer()
  val getHttpLocalhostGatewaySuffix = SuspendOnce { ".localhost:${startServer()}" }

  suspend fun getUrl() = "http://127.0.0.1:${startServer()}"

}

val dwebHttpGatewayServer get() = DwebHttpGatewayServer.INSTANCE