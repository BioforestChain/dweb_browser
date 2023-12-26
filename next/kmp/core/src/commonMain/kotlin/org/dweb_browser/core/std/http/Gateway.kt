package org.dweb_browser.core.std.http

import io.ktor.http.HttpStatusCode
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureServerRequest
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.ReadableStreamIpc
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.helper.SimpleCallback
import org.dweb_browser.helper.SimpleSignal

class Gateway(
  val listener: PortListener, val urlInfo: HttpNMM.ServerUrlInfo, val token: String
) {

  class PortListener(
    val mainIpc: Ipc, val host: String
  ) {
    private val _routerSet = mutableSetOf<StreamIpcRouter>();

    fun addRouter(config: CommonRoute, ipc: Ipc): () -> Boolean {
      val route = StreamIpcRouter(config, ipc);
      this._routerSet.add(route)
      return {
        this._routerSet.remove(route)
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
    private val destroySignal = SimpleSignal()
    fun onDestroy(cb: SimpleCallback) = destroySignal.listen(cb)

    suspend fun destroy() {
      _routerSet.map {
        when (val ipc = it.ipc) {
          is ReadableStreamIpc -> ipc.input.closeRead()
          else -> ipc.close()
        }
      }
      destroySignal.emit()
    }
  }

  class StreamIpcRouter(val config: CommonRoute, val ipc: Ipc) {
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
