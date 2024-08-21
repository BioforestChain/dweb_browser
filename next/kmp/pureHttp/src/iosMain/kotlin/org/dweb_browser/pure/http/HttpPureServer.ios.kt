package org.dweb_browser.pure.http

import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.commonAsyncExceptionHandler
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.pure.http.ktor.KtorPureServer

actual class HttpPureServer actual constructor(onRequest: HttpPureServerOnRequest) :
  KtorPureServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>(CIO, onRequest) {
  init {
    allHttpPureServerInstances.add(this)
  }

  override fun getCoroutineExceptionHandler(): CoroutineExceptionHandler {
    return CoroutineExceptionHandler { ctx, e ->
      if (e.message?.contains("ENOTCONN (57)") == true) {
        globalDefaultScope.launch(start = CoroutineStart.UNDISPATCHED) {
          WARNING("网络枢纽被关闭: ${e.message} currentPort=${getPort()}")
          this@HttpPureServer.apply {
            close()
            WARNING("即将自动重启网络枢纽 currentPort=${getPort()}")
            start(0u)
            WARNING("重启完成 currentPort=${getPort()}")
          }
        }
      } else {
        commonAsyncExceptionHandler.handleException(ctx, e)
      }
    }
  }
}