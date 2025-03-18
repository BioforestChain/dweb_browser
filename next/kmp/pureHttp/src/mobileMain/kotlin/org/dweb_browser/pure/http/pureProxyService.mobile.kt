package org.dweb_browser.pure.http

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.http.dwebHttpGatewayService
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.helper.eprintln
import org.dweb_browser.helper.globalIoScope
import org.dweb_browser.pure.http.onPortChange
import org.dweb_browser.reverse_proxy.VoidCallback


actual class DwebProxyService actual constructor() {
  private val lock = Mutex()
  private var runningJob: Job? = null
  suspend fun start() = lock.withLock {
    // 避免重复启动
    if (runningJob != null) {
      return@withLock
    }
    var job: Job? = null
    runningJob = (globalIoScope + CoroutineExceptionHandler { ctx, throwable ->
      eprintln("QWQ ReverseProxy die~~")
      eprintln(throwable.stackTraceToString())
      job?.cancel("ReverseProxy Shutdown", throwable)
    }).launch {
      /// 如果异常，持续自动重启
      while (true) {
        try {
          debugDWebView("reverse_proxy", "starting")
          var backendServerPort = dwebHttpGatewayService.getPort()

          val proxyReadyCallback = object : VoidCallback {
            override fun callback(proxyPort: UShort, frontendPort: UShort) {
              debugDWebView("reverse_proxy") {
                "running proxyServerPort=${proxyPort}, frontendServerPort=${frontendPort}, backendServerPort=${backendServerPort}"
              }
              proxyUrlFlow.value = "http://127.0.0.1:${proxyPort}"

              job = dwebHttpGatewayService.server.onPortChange(
                "ReverseProxySetForward",
                false
              ) { newPort ->
                debugDWebView("DwebViewProxy/onPortChange") {
                  "backendServerPort=$backendServerPort, newPort=$newPort"
                }
                if (backendServerPort != newPort) {
                  backendServerPort = newPort
                  org.dweb_browser.reverse_proxy.forward(newPort)
                  debugDWebView("DwebViewProxy/onPortChange", "ReverseProxySetForward done")
                }
              }
            }
          }
          org.dweb_browser.reverse_proxy.start(backendServerPort, proxyReadyCallback)
          debugDWebView("reverse_proxy", "stopped")
        } catch (e: Throwable) {
          debugDWebView("reverse_proxy", "error", e)
        } finally {
          job?.cancel()
          job = null
          proxyUrlFlow.value = null
          delay(1000)// 减少自动重启的频率
        }
      }
    }
  }

  suspend fun stop() = lock.withLock {
    runningJob?.cancel()
    runningJob = null
    proxyUrlFlow.value = null
  }

  actual val proxyUrlFlow = MutableStateFlow<String?>(null)

//  actual val proxyUrlFlow: StateFlow<String?>
//    get() = TODO("Not yet implemented")
}