package info.bagen.rust.plaoc.microService.network

import info.bagen.rust.plaoc.microService.Gateway
import info.bagen.rust.plaoc.microService.moduleRouter
import io.ktor.http.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
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

    val tokenMap = mutableMapOf</* token */ String, Gateway>()
    val gatewayMap = mutableMapOf</* host */ String, Gateway>()

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
            val userAgent = call.request.headers["User-Agent"]
            println("RequestHookPlugin#request User-Agent ==> $userAgent")
            /// 在网关中寻址能够处理该 host 的监听者
//            val gateway = gatewayMap[userAgent]
//                ?: return@onCall call.respond(
//                    DefaultErrorResponse(
//                        502,
//                        "Bad Gateway",
//                        "作为网关或者代理工作的服务器尝试执行请求时，从远程服务器接收到了一个无效的响应"
//                    )
//                )
//
//            /* 30s 没有任何 body 写入的话，认为网关超时 */
//            gateway.listener.hookHttpRequest(call.request, call.response)
        }
    }


    fun createServer() {
        CoroutineScope(Dispatchers.IO).launch {
            /** 如果为真，则 start 调用会阻塞当前线程，直到它完成执行。
             * 如果您使用 wait = false 从主线程开始运行并且没有其他任何东西阻塞该线程，
             * 那么您的应用程序将在不处理任何请求的情况下终止。*/
            server.start(wait = true)
        }
    }

    fun closeServer() {
        // gracePeriodMillis - 活动冷却的最长时间
        // timeoutMillis - 等待服务器正常停止的最长时间
        CoroutineScope(Dispatchers.IO).launch {
            server.stop(1_000, 2_000)
        }
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
