package org.dweb_browser.core.std.http.net

import org.dweb_browser.core.http.HttpGateway
import org.dweb_browser.core.http.dwebHttpGatewayService
import org.dweb_browser.core.std.http.debugHttp
import org.dweb_browser.pure.http.onPortChange

class Http1Server {
  companion object {
    const val PREFIX = "http://";
    const val PROTOCOL = "http:";
    const val PORT = 80;
  }

  private var bindingPort = 0

  init {
    dwebHttpGatewayService.server.onPortChange("Http1Server") {
      bindingPort = it.toInt()
      debugHttp("Http1Server/onPortChange") { "bindingPort=$bindingPort" }
    }
  }

  suspend fun createServer(httpHandler: HttpGateway) {
    dwebHttpGatewayService.gatewayAdapterManager.append(adapter = httpHandler)

    bindingPort = dwebHttpGatewayService.getPort().toInt()
  }

  val authority get() = "localhost:$bindingPort"
  val origin get() = "$PREFIX$authority"

  suspend fun closeServer() {
    dwebHttpGatewayService.server.close()
  }
}


