package org.dweb_browser.core.std.http.net

import io.ktor.server.engine.ApplicationEngine
import io.ktor.util.InternalAPI
import org.dweb_browser.core.http.DwebHttpGatewayServer
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.dwebHttpGatewayServer
import org.dweb_browser.core.std.http.Gateway

typealias GatewayHandler = suspend (request: PureRequest) -> Gateway?
typealias GatewayHttpHandler = suspend (gateway: Gateway, request: PureRequest) -> PureResponse?
typealias GatewayErrorHandler = suspend (request: PureRequest, gateway: Gateway?) -> PureResponse


class Http1Server {
  companion object {
    const val PREFIX = "http://";
    const val PROTOCOL = "http:";
    const val PORT = 80;
  }

  private var bindingPort = -1

  private var server: ApplicationEngine? = null

  @OptIn(InternalAPI::class)
  suspend fun createServer(
    gatewayHandler: GatewayHandler,
    httpHandler: GatewayHttpHandler,
    errorHandler: GatewayErrorHandler
  ) {
    if (server != null) {
      throw Exception("server alter created")
    }
    DwebHttpGatewayServer.gatewayAdapterManager.append { request ->
      when (val gateway = gatewayHandler(request)) {
        null -> errorHandler(request, null)
        else -> httpHandler(gateway, request) ?: errorHandler(request, gateway)
      }
    }

    dwebHttpGatewayServer.startServer()
  }

  val authority get() = "localhost:$bindingPort"
  val origin get() = "$PREFIX$authority"

  fun closeServer() {
    server?.also {
      it.stop()
      server = null
    } ?: throw Exception("server not created")
  }
}


