package org.dweb_browser.microservice.sys.http

import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.helper.IpcMethod
import org.dweb_browser.microservice.ipc.ReadableStreamIpc
import org.dweb_browser.helper.*
import io.ktor.util.collections.*
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsMessage

class Gateway(
  val listener: PortListener, val urlInfo: HttpNMM.ServerUrlInfo, val token: String
) {

  class PortListener(
    val ipc: Ipc,
    val host: String
  ) {
    private val _routerSet = ConcurrentSet<StreamIpcRouter>();

    fun addRouter(config: RouteConfig, streamIpc: ReadableStreamIpc): (Unit) -> Boolean {
      val route = StreamIpcRouter(config, streamIpc);
      this._routerSet.add(route)
      return {
        this._routerSet.remove(route)
      }
    }

    /**
     * 接收 nodejs-web 请求
     * 将之转发给 IPC 处理，等待远端处理完成再代理响应回去
     */
    suspend fun hookHttpRequest(request: Request): Response? {
      println("httpHandler=>  ${request.uri} ")
      for (router in _routerSet) {
        val response = router.handler(request)
        if (response != null) {
          return response
        }
      }
      return null
    }

    suspend fun hookWsRequest(request: Request): WsConsumer? {
      for (router in _routerSet) {
        val response = router.handler(request)
        if (response != null) {
          return { ws -> ws.send(WsMessage(response.body)) }
        }
      }
      return null
    }

    /// 销毁
    private val destroySignal = SimpleSignal()
    fun onDestroy(cb: SimpleCallback) = destroySignal.listen(cb)

    suspend fun destroy() {
      _routerSet.map {
        it.streamIpc.stream.close()
      }
      destroySignal.emit()
    }
  }

  data class RouteConfig(
    val pathname: String,
    val method: IpcMethod,
    val matchMode: MatchMode = MatchMode.PREFIX
  )

  class StreamIpcRouter(val config: RouteConfig, val streamIpc: ReadableStreamIpc) {

    val isMatch: (request: Request) -> Boolean by lazy {
      when (config.matchMode) {
        MatchMode.PREFIX -> { request ->
          request.method == config.method.http4kMethod && request.uri.path.startsWith(
            config.pathname
          )
        }

        MatchMode.FULL -> { request ->
          request.method == config.method.http4kMethod && request.uri.path == config.pathname
        }
      }
    }

    suspend fun handler(request: Request) = if (isMatch(request)) {
      streamIpc.request(request)
    } else if (request.method == Method.OPTIONS) {
      // 处理options请求
      Response(Status.OK).headers(
        CORS_HEADERS
      )
    } else null
  }
}

val CORS_HEADERS = listOf(
  Pair("Access-Control-Allow-Origin", "*"),
  Pair("Access-Control-Allow-Headers", "*"),
  Pair("Access-Control-Allow-Methods", "*"),
)
