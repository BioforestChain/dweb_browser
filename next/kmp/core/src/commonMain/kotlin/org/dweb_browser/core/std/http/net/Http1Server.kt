package org.dweb_browser.core.std.http.net

import org.dweb_browser.core.http.DwebHttpGatewayServer
import org.dweb_browser.core.http.dwebHttpGatewayServer
import org.dweb_browser.core.std.http.Gateway
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureServerRequest

typealias GatewayHandler = suspend (request: PureServerRequest) -> Gateway?
typealias GatewayHttpHandler = suspend (gateway: Gateway, request: PureServerRequest) -> PureResponse?
typealias GatewayErrorHandler = suspend (request: PureServerRequest, gateway: Gateway?) -> PureResponse


class Http1Server {
  companion object {
    const val PREFIX = "http://";
    const val PROTOCOL = "http:";
    const val PORT = 80;
  }

  private var bindingPort = -1

  suspend fun createServer(
    gatewayHandler: GatewayHandler,
    httpHandler: GatewayHttpHandler,
    errorHandler: GatewayErrorHandler,
  ) {
    DwebHttpGatewayServer.gatewayAdapterManager.append { request ->
      when (val gateway = gatewayHandler(request)) {
        null -> errorHandler(request, null)
        else -> httpHandler(gateway, request) ?: errorHandler(request, gateway)
      }
    }

    bindingPort = dwebHttpGatewayServer.startServer()
  }

  val authority get() = "localhost:$bindingPort"
  val origin get() = "$PREFIX$authority"

  suspend fun closeServer() {
    dwebHttpGatewayServer.closeServer()
  }
}


