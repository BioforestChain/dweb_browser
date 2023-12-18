package org.dweb_browser.dwebview.proxy

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.http.dwebHttpGatewayServer
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.ioAsyncExceptionHandler
import reverse_proxy.VoidCallback

object DwebViewProxy {
  val prepare = SuspendOnce {
    val proxyAddress = CompletableDeferred<String>()
    CoroutineScope(ioAsyncExceptionHandler).launch {
      debugDWebView("reverse_proxy", "starting")
      val backendServerPort = dwebHttpGatewayServer.startServer().toUShort()

      val proxyReadyCallback = object : VoidCallback {
        override fun callback(proxyPort: UShort, frontendPort: UShort) {
          debugDWebView(
            "reverse_proxy",
          ) {
            "running proxyServerPort=${proxyPort}, frontendServerPort=${frontendPort}, backendServerPort=${backendServerPort}"
          }
          proxyAddress.complete("http://127.0.0.1:${proxyPort}")
        }
      }
      reverse_proxy.start(backendServerPort, proxyReadyCallback)
      debugDWebView("reverse_proxy", "stopped")
    }
    ProxyUrl = proxyAddress.await()
  }

  lateinit var ProxyUrl: String
    private set
}