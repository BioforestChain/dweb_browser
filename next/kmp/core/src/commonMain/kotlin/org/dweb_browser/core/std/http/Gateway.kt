package org.dweb_browser.core.std.http

import io.ktor.http.HttpStatusCode
import io.ktor.util.collections.ConcurrentSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.helper.DeferredSignal
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureServerRequest

internal data class Gateway(
  val listener: PortListener, val urlInfo: ServerUrlInfo, val token: String,
) {

  data class PortListener(
    val mainIpc: Ipc, val host: String,
  ) {
    private val _routerSet = ConcurrentSet<StreamIpcRouter>()
    
    fun addRouter(config: CommonRoute, ipc: Ipc) {
      val route = StreamIpcRouter(config, ipc)
      _routerSet.add(route)
      ipc.onClosed {
        _routerSet.remove(route)
      }
    }

    /**
     * 接收 nodejs-web 请求
     * 将之转发给 IPC 处理，等待远端处理完成再代理响应回去
     */
    suspend fun hookHttpRequest(request: PureServerRequest): PureResponse? {
      for (router in _routerSet) {
        val response = router.handler(request)
        if (response != null) {
          return response
        }
      }
      return null
    }

    /// 销毁
    val destroyDeferred = CompletableDeferred<CancellationException?>()
    val onDestroy = DeferredSignal(destroyDeferred)

    suspend fun destroy(cause: CancellationException? = null) {
      debugHttp("destroy") { "host=$host routeCount=${_routerSet.size}" }
      _routerSet.toList().forEachIndexed { index, streamIpcRouter ->
        debugHttp("destroy/closeIpc") { "index=$index ipc=${streamIpcRouter.ipc}" }
        streamIpcRouter.ipc.close(cause)
      }
      _routerSet.clear()
      debugHttp("destroy/closeIpc", "done")
      destroyDeferred.complete(cause)
    }
  }

  data class StreamIpcRouter(val config: CommonRoute, val ipc: Ipc) {
    suspend fun handler(request: PureServerRequest) = if (config.isMatch(request)) {
      /// 这里的 request 并不是 pureClientRequest，而是 pureServerRequest
      ipc.request(request.toClient())
    } else if (request.method == PureMethod.OPTIONS && request.url.host != "http.std.dweb") {
      // 处理options请求
      PureResponse(HttpStatusCode.OK, PureHeaders(CORS_HEADERS.toMap()))
    } else null
  }
}

val CORS_HEADERS = listOf(
  Pair("Access-Control-Allow-Origin", "*"),
  Pair("Access-Control-Allow-Headers", "*"),
  Pair("Access-Control-Allow-Methods", "*"),
)
