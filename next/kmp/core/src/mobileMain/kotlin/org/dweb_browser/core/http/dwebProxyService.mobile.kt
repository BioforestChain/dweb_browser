package org.dweb_browser.core.http

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.std.http.debugHttp
import org.dweb_browser.helper.eprintln
import org.dweb_browser.helper.globalIoScope
import org.dweb_browser.pure.http.onPortChange
import org.dweb_browser.reverse_proxy.VoidCallback

actual class DwebProxyService actual constructor() {
  private val lock = Mutex()
  private var runningJob: Job? = null
  actual suspend fun start() = lock.withLock {
    // 避免重复启动
    if (runningJob != null) {
      return@withLock
    }
    var job: Job? = null
    runningJob = (globalIoScope + CoroutineExceptionHandler { ctx, throwable ->
      eprintln("QWQ ReverseProxy die~~")
      eprintln(throwable.stackTraceToString())
      job?.cancel("ReverseProxy Shutdown", throwable)
      proxyUrlFlow.value = null
    }).launch {
      /// 如果异常，持续自动重启
      while (true) {
        try {
          debugHttp("reverse_proxy", "starting")
          var backendServerPort = dwebHttpGatewayService.getPort()
          val proxyReadyCallback = object : VoidCallback {
            override fun callback(proxyPort: UShort, frontendPort: UShort) {
              debugHttp("reverse_proxy") {
                "running proxyServerPort=${proxyPort}, frontendServerPort=${frontendPort}, backendServerPort=${backendServerPort}"
              }
              proxyUrlFlow.value = "http://127.0.0.1:${proxyPort}"

              job = dwebHttpGatewayService.server.onPortChange(
                "ReverseProxySetForward",
                false
              ) { newPort ->
                debugHttp("DwebViewProxy/onPortChange") {
                  "backendServerPort=$backendServerPort, newPort=$newPort"
                }
                if (backendServerPort != newPort) {
                  backendServerPort = newPort
                  org.dweb_browser.reverse_proxy.forward(newPort)
                  debugHttp("DwebViewProxy/onPortChange", "ReverseProxySetForward done")
                }
              }
            }
          }
          org.dweb_browser.reverse_proxy.start(DWEB_SSL_PEM, backendServerPort, proxyReadyCallback)
          debugHttp("reverse_proxy", "stopped")
        } catch (e: Throwable) {
          debugHttp("reverse_proxy", "error", e)
        } finally {
          job?.cancel()
          job = null
          proxyUrlFlow.value = null
          delay(1000)// 减少自动重启的频率
        }
      }
    }
  }

  actual suspend fun stop() = lock.withLock {
    runningJob?.cancel()
    runningJob = null
    proxyUrlFlow.value = null
  }

  private val proxyUrlFlow = MutableStateFlow<String?>(null)
  actual val proxyUrl by lazy { proxyUrlFlow.asStateFlow() }
}