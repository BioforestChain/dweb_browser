package org.dweb_browser.core.std.http.net

import org.dweb_browser.core.http.DwebHttpGatewayServer
import org.dweb_browser.core.http.HttpGateway
import org.dweb_browser.core.http.dwebHttpGatewayServer

class Http1Server {
  companion object {
    const val PREFIX = "http://";
    const val PROTOCOL = "http:";
    const val PORT = 80;
  }

  private var bindingPort = -1

  suspend fun createServer(httpHandler: HttpGateway) {
    DwebHttpGatewayServer.gatewayAdapterManager.append(adapter = httpHandler)

    bindingPort = dwebHttpGatewayServer.startServer()
  }

  val authority get() = "localhost:$bindingPort"
  val origin get() = "$PREFIX$authority"

  suspend fun closeServer() {
    dwebHttpGatewayServer.closeServer()
  }
}


