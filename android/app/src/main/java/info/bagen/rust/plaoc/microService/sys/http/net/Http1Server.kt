package info.bagen.rust.plaoc.microService.sys.http.net

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.http4k.core.*
import org.http4k.server.Http4kServer
import org.http4k.server.Netty
import org.http4k.server.asServer


class Http1Server {
    companion object {
        const val PREFIX = "http://";
        const val PROTOCOL = "http:";
        const val PORT = 80;
    }

    var bindingPort = 22605

    private var server: Http4kServer? = null
    fun createServer(handler: HttpHandler) {
        if (server != null) {
            throw Exception("server alter created")
        }
        CoroutineScope(Dispatchers.IO).launch {
            server = handler.asServer(Netty(0/* 使用随机端口*/)).start().also { server ->
                bindingPort = server.port()
            }
        }
    }

    fun closeServer() {
        server?.also {
            it.close()
            server = null
        } ?: throw Exception("server not created")
    }
}


