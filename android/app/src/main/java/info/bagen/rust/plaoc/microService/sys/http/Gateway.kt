package info.bagen.rust.plaoc.microService.sys.http

import info.bagen.rust.plaoc.microService.helper.SimpleCallback
import info.bagen.rust.plaoc.microService.helper.SimpleSignal
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.IpcMethod
import info.bagen.rust.plaoc.microService.ipc.ReadableStreamIpc
import io.ktor.util.collections.*
import org.http4k.core.Request
import org.http4k.core.Response

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
        fun onDestroy(cb: SimpleCallback) = destroySignal.listen { cb }

        suspend fun destroy() {
            _routerSet.clear()
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
        } else null
    }


}

