package org.dweb_browser.core.http

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.std.http.debugHttp
import org.dweb_browser.helper.eprintln
import org.dweb_browser.helper.globalIoScope
import org.dweb_browser.pure.http.ReverseProxyServer

actual class DwebProxyService actual constructor() {
  private val reverseProxyServer = ReverseProxyServer()
  private val lock = Mutex()
  private var runningJob: Job? = null
  actual suspend fun start() = lock.withLock {
    // 避免重复启动
    if (runningJob != null) {
      return@withLock
    }
    runningJob = (globalIoScope + CoroutineExceptionHandler { ctx, throwable ->
      eprintln("QWQ ReverseProxy die~~")
      eprintln(throwable.stackTraceToString())
      proxyUrlFlow.value = null
    }).launch {
      debugHttp("reverse_proxy", "starting")
      val backendServerPort = dwebHttpGatewayService.getPort()
      val proxyPort = reverseProxyServer.start(backendServerPort)
      debugHttp("reverse_proxy") {
        "running proxyPort=${proxyPort},  backendServerPort=${backendServerPort}"
      }
      proxyUrlFlow.value = "http://127.0.0.1:${proxyPort}"
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