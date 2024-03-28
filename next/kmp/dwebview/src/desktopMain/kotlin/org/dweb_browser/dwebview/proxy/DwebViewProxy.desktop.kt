package org.dweb_browser.dwebview.proxy


import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.http.dwebHttpGatewayServer
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.pure.http.ReverseProxyServer

object DwebViewProxy {
  val prepare = SuspendOnce {
    val reverseProxyServer = ReverseProxyServer()
    val proxyAddress = CompletableDeferred<String>()
    CoroutineScope(ioAsyncExceptionHandler).launch {
      debugDWebView("reverse_proxy", "starting")
      val backendServerPort = dwebHttpGatewayServer.startServer().toUShort()
      val proxyPort = reverseProxyServer.start(backendServerPort)
      debugDWebView("reverse_proxy") {
        "running proxyPort=${proxyPort},  backendServerPort=${backendServerPort}"
      }
      proxyAddress.complete("http://127.0.0.1:${proxyPort}")
    }
    ProxyUrl = proxyAddress.await()
  }

  lateinit var ProxyUrl: String
    private set
}