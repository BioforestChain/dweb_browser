package org.dweb_browser.js_backend.http

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import node.http.createServer
import node.http.IncomingMessage
import node.http.ServerResponse
import node.http.Server
import node.url.parse

/**
 * 单例模式全局只允许有一个httpServer
 */
class HttpServer private constructor(){
    val scope = CoroutineScope(Dispatchers.Default)
    private var _port: Int = 8888
    private val _server = createServer(::requestListener)
    private lateinit var _address: String
    private var _isStart = false
    private val _router = Router()

    private fun requestListener(req: IncomingMessage, res: ServerResponse<*>){
        // http://demo.compose.app.localhost:8888/index.html
        val subDomain = req.headers.host?.split(".localhost")?.get(0)?:""
        val reqMethod = req.method?:throw(Throwable("""
            req.method == null
            req.method: ${req.method}
            at requestListener
            at HttpServer
        """.trimIndent()))
        val route = req.url?.let { parse(it,false)}?.pathname?.let { reqPath ->
            _router.getAllRoutes().values.firstOrNull {
                it.hasMatch(reqPath, reqMethod, subDomain)
            }
        }
        when(route){
            null -> res.notFound()
            else -> route(req, res)
        }
    }

    // TODO: 这里也必须要保证值值调用一次
    fun start(port: Int = 8888, listeningListener: ( Server<IncomingMessage, ServerResponse<*>>.() -> Unit)? = null): HttpServer{
        _port = port
        return start(listeningListener)
    }

    fun start(listeningListener: ( Server<IncomingMessage, ServerResponse<*>>.() -> Unit)? = null): HttpServer{
        _isStart = true;
        _server.listen(_port){
            _address = "http://localhost:${_server.address().asDynamic().port}"
            console.log(_address)
            if(listeningListener != null) {
                listeningListener(_server)
            }
        }
        return this
    }

    fun getAddress(): String{
        return _address
    }

    fun getServer() = _server

    // TODO: 需要添加一个判断 - 如果之前的路由已经有了相同匹配条件就不能够重复添加
    // path ， method , matchPattern 相同
    fun routeAdd(route: Route) = _router.add(route)
    fun routeRemove(path: String) = _router.remove(path)
    fun routeRemove(route: Route) = _router.remove(route.path)

    companion object{
        private val mutex = Mutex()
        val deferredInstance = CompletableDeferred<HttpServer>()
        suspend fun createHttpServer(): CompletableDeferred<HttpServer>{
            mutex.withLock {
                if (!deferredInstance.isCompleted) {
                    deferredInstance.complete(HttpServer())
                }
            }
            return deferredInstance
        }
    }
}


