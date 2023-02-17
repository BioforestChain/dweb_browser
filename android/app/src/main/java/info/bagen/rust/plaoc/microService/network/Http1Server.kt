package info.bagen.rust.plaoc.microService.network

import info.bagen.rust.plaoc.microService.Gateway
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.http4k.core.*
import org.http4k.server.Http4kServer
import org.http4k.server.Netty
import org.http4k.server.PolyHandler
import org.http4k.server.asServer
import org.http4k.sse.SseFilter
import org.http4k.sse.SseHandler


class Http1Server {
    companion object {
        const val PREFIX = "http://";
        const val PROTOCOL = "http:";
        const val PORT = 80;
    }

    private var bindingPort = 24433

    private var server: Http4kServer? = null
    fun createServer(sse:  SseHandler) {
        if (server != null) {
            throw Exception("server alter created")
        }
        val app = { request: Request -> Response(Status.OK).body("Hello, ${request.query("name")}!") }

        CoroutineScope(Dispatchers.IO).launch {
            PolyHandler(app, sse = sse).asServer(Netty(bindingPort)).start()
        }
    }

    fun closeServer() {
        server?.also {
            it.close()
            server = null
        } ?: throw Exception("server not created")
    }
}


