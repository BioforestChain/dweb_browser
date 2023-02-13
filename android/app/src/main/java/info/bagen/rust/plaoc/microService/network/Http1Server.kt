package info.bagen.rust.plaoc.microService.network

import info.bagen.rust.plaoc.microService.moduleRouter
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
import kotlinx.serialization.json.Json


class Http1Server {
    companion object {
        private const val PORT = 24433
    }

    private val server by lazy {
        embeddedServer(Netty, port = PORT, watchPaths = emptyList()) {
            install(WebSockets)
            install(CallLogging)
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true // 标准的缩进
                    isLenient = true //在宽松模式下，引用的布尔文字和未引用的字符串文字是允许的
                })
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
            module()
        }
    }

    fun createServer() {
        CoroutineScope(Dispatchers.IO).launch {
            server.start(wait = true)
        }
    }

    fun closeServer() {
        server.stop()
    }
}

fun Application.module() {
    moduleRouter()
    modulePlugin()
}

fun Application.modulePlugin() {
    install(ShutDownUrl.ApplicationCallPlugin) {
        shutDownUrl = "/shutdown"
        exitCodeSupplier = { 0 }
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
