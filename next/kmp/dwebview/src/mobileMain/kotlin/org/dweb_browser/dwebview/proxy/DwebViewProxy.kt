package org.dweb_browser.dwebview.proxy

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.dweb_browser.core.http.dwebHttpGatewayServer
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.globalIoScope
import reverse_proxy.VoidCallback

object DwebViewProxy {
  val waitReady = SuspendOnce {
    proxyUrl = proxyUrlFlow.filterNotNull().first()
  }

  val proxyUrlFlow by lazy {
    MutableStateFlow<String?>(null).also { flow ->
      globalIoScope.launch {
        /// 持续自动重启
        while (true) {
          try {
            debugDWebView("reverse_proxy", "starting")
            val backendServerPort = dwebHttpGatewayServer.startServer().toUShort()

            val proxyReadyCallback = object : VoidCallback {
              override fun callback(proxyPort: UShort, frontendPort: UShort) {
                debugDWebView("reverse_proxy") {
                  "running proxyServerPort=${proxyPort}, frontendServerPort=${frontendPort}, backendServerPort=${backendServerPort}"
                }
                flow.value = "http://127.0.0.1:${proxyPort}"
              }
            }
            reverse_proxy.start(backendServerPort, proxyReadyCallback)
            debugDWebView("reverse_proxy", "stopped")
          } catch (e: Throwable) {
            debugDWebView("reverse_proxy", "error", e)
          } finally {
            flow.value = null
            delay(1000)// 减少自动重启的频率
          }
        }
      }
    }
  }
  lateinit var proxyUrl: String
    private set
}