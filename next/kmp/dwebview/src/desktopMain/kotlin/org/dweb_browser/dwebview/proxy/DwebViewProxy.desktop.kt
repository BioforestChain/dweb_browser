package org.dweb_browser.dwebview.proxy


import kotlinx.coroutines.async
import org.dweb_browser.core.http.dwebHttpGatewayServer
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.globalIoScope
import org.dweb_browser.pure.http.ReverseProxyServer

object DwebViewProxy {
  private val reverseProxyServer = ReverseProxyServer()
  val prepare = SuspendOnce {
    proxyUrl = globalIoScope.async {
      debugDWebView("reverse_proxy", "starting")
      val backendServerPort = dwebHttpGatewayServer.startServer().toUShort()
      val proxyPort = reverseProxyServer.start(backendServerPort)
      debugDWebView("reverse_proxy") {
        "running proxyPort=${proxyPort},  backendServerPort=${backendServerPort}"
      }
      "http://127.0.0.1:${proxyPort}"
    }.await()
  }

  init {
    dwebHttpGatewayServer.onClosed {
      debugDWebView("reverse_proxy", "reset")
      prepare.reset()
      reverseProxyServer.close()
    }
  }

  lateinit var proxyUrl: String
    private set
}