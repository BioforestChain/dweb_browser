package info.bagen.rust.plaoc.microService.network

import info.bagen.rust.plaoc.microService.Gateway
import info.bagen.rust.plaoc.microService.moduleRouter
import io.ktor.http.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class Http1Server {
    companion object {
         const val PORT = 24433
    }
    val tokenMap = mutableMapOf</* token */ String, Gateway>();
    val gatewayMap = mutableMapOf</* host */ String, Gateway>();

    private val server by lazy {
        embeddedServer(Netty, port = PORT, watchPaths = emptyList()) {
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
            module()
        }
    }

    // 监听请求
    private val requestHookPlugin = createApplicationPlugin(name = "RequestHookPlugin") {
        onCall { call ->
            call.response.headers.append("User-Agent", "Hello, world!")
            println("RequestHookPlugin#origin==> ${call.request.origin}")
            println("RequestHookPlugin#request User-Agent ==> ${call.request.headers["User-Agent"]}")
            println("RequestHookPlugin#response User-Agent ==> ${call.response.headers["User-Agent"]}")
            /// 在网关中寻址能够处理该 host 的监听者
            val gateway = gatewayMap[call.request.headers["User-Agent"]]
                ?: return@onCall call.respond(DefaultErrorResponse(
                    502,
                    "Bad Gateway",
                    "作为网关或者代理工作的服务器尝试执行请求时，从远程服务器接收到了一个无效的响应"
                ));

            /* 30s 没有任何 body 写入的话，认为网关超时 */
            gateway.listener.hookHttpRequest(call.request, call.response)
            call.request.origin.apply {
                println("Request URL: $scheme://$localHost:$localPort$uri")
            }
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
