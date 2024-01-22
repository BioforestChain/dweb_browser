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

class HttpServer private constructor(){
    val scope = CoroutineScope(Dispatchers.Default)
    private var _port: Int = 8888
    private val _server = createServer(::requestListener)
    private lateinit var _address: String
    private var _isStart = false
    private val _router = Router()

    private fun requestListener(req: IncomingMessage, res: ServerResponse<*>){
        val reqMethod = req.method?:throw(Throwable("""
            req.method == null
            req.method: ${req.method}
            at requestListener
            at HttpServer
        """.trimIndent()))
        val route = req.url?.let { parse(it,false)}?.pathname?.let { reqPath ->
            _router.getAllRoutes().values.firstOrNull{
                it.hasMatch(reqPath, reqMethod)
            }
        }?:throw(Throwable("""
            没有匹配的路由监听器
            req.url = ${req.url}
            at requestListener
            at HttpServer
        """.trimIndent()))
        route(req, res)
    }

    // TODO: 这里也必须要保证值值调用一次
    fun start(port: Int = 8888, listeningListener: ( Server<IncomingMessage, ServerResponse<*>>.() -> Unit)? = null): HttpServer{
        _port = port
        return start(listeningListener)
    }

    fun start(listeningListener: ( Server<IncomingMessage, ServerResponse<*>>.() -> Unit)? = null): HttpServer{
        _isStart = true;
        _server.listen(_port){
            _address = "http://127.0.0.1:${_server.address().asDynamic().port}"
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
