package org.dweb_browser.js_backend.http

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import node.http.createServer
import node.http.IncomingMessage
import node.http.ServerResponse
import node.http.RequestListener
import node.http.Server

class HttpServer private constructor(listener: RequestListener<IncomingMessage, ServerResponse<*>>){
    val scope = CoroutineScope(Dispatchers.Default)
    private val server = createServer(listener)
    private lateinit var _address: String
    private var _isStart: Boolean = false
    fun start(port: Number = 8888, listeningListener: ( Server<IncomingMessage, ServerResponse<*>>.() -> Unit)? = null): HttpServer{
        _isStart = true;
        server.listen(port){
            _address = "http://127.0.0.1:${server.address().asDynamic().port}"
            console.log(_address)
            if(listeningListener != null) {
                listeningListener(server)
            }
        }
        return this
    }

    fun getAddress(): String{
        return _address
    }

    fun getServer() = server

    companion object{
        val deferredInstance = CompletableDeferred<HttpServer>()
        fun createHttpServer(listener: RequestListener<IncomingMessage, ServerResponse<*>>): CompletableDeferred<HttpServer>{
            return if(deferredInstance.isCompleted){
                deferredInstance
            }else{
                deferredInstance.complete(HttpServer(listener))
                deferredInstance
            }
        }
    }
}




