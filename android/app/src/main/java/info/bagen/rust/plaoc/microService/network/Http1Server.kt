package info.bagen.rust.plaoc.microService.network

import info.bagen.rust.plaoc.microService.Gateway
import io.ktor.http.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class Http1Server {
    companion object {
        const val PREFIX = "http://";
        const val PROTOCOL = "http:";
        const val PORT = 80;
    }

    private var bindingPort = 24433

    val tokenMap = mutableMapOf</* token */ String, Gateway>();
    val gatewayMap = mutableMapOf</* host */ String, Gateway>();


    private var server: ApplicationEngine? = null

    fun createServer(requestHookPlugin: ApplicationPlugin<Unit>) {
        if (server != null) {
            throw Exception("server created")
        }

        CoroutineScope(Dispatchers.IO).launch {
            server = embeddedServer(Netty, port = bindingPort, watchPaths = emptyList()) {
                install(WebSockets)
                install(requestHookPlugin)
                install(CallLogging)
                install(ContentNegotiation) {
                    json()
                }
                install(CORS) {
                    allowMethod(HttpMethod.Options)
                    allowMethod(HttpMethod.Put)
                    allowMethod(HttpMethod.Patch)
                    allowMethod(HttpMethod.Delete)
                    anyHost()
                }
                // 压缩内容
                install(Compression) {
                    gzip()
                }
            }.also {
                it.start(wait = true)
            }
        }
    }

    fun closeServer() {
        server?.also {
            it.stop()
            server = null
        } ?: throw Exception("server not created")
    }
}



fun rand(start: Int, end: Int): Int {
    require(start <= end) { "Illegal Argument" }
    return (start..end).random()
}

data class HttpRequestInfo(
    var http_req_id: Number,
    var url: String,
    var method: HttpMethod,
    var rawHeaders: MutableList<String> = mutableListOf()
)

data class HttpResponseInfo(
    var http_req_id: Number,
    var statusCode: Number,
    var headers: Map<String, String>,
    var body: Any
)

data class HttpListener(
    var host: String = ""
) {
    private val protocol = "https://"
    val origin = "$protocol${this.host}.${rand(0, 25535)}.localhost"

    fun getAvailablePort(): Number {
        return 25535
    }
}
