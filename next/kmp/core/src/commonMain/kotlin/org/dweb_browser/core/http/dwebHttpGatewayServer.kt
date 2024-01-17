package org.dweb_browser.core.http


import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.help.AdapterManager
import org.dweb_browser.core.std.http.findDwebGateway
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
    val rawUrl = rawRequest.href
    val url = rawRequest.queryOrNull("X-Dweb-Url") ?: when (val info =
      findDwebGateway(rawRequest)) {
      null -> rawUrl
      else -> "${info.protocol.name}://${info.host}$rawUrl"
    }
    var pureRequest = if (url != rawUrl) rawRequest.copy(href = url) else rawRequest;
    try {
      gatewayAdapterManager.doGateway(pureRequest)
        ?: PureResponse(HttpStatusCode.GatewayTimeout)
    } catch (e: Throwable) {
      PureResponse(HttpStatusCode.BadGateway, body = IPureBody.from(e.message ?: ""))
    }
  }

  val startServer = SuspendOnce {
    server.start(0u).toInt()
  }

  suspend fun getPort() = startServer()
  val getHttpLocalhostGatewaySuffix = SuspendOnce { ".localhost:${startServer()}" }

  suspend fun getUrl() = "http://127.0.0.1:${startServer()}"

}

val dwebHttpGatewayServer get() = DwebHttpGatewayServer.INSTANCE