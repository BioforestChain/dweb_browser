package info.bagen.rust.plaoc.microService

import android.os.Build
import info.bagen.rust.plaoc.microService.network.DefaultErrorResponse
import info.bagen.rust.plaoc.microService.network.Http1Server
import info.bagen.rust.plaoc.microService.network.Http1Server.Companion.PORT
import info.bagen.rust.plaoc.microService.network.PortListener
import info.bagen.rust.plaoc.microService.route.jsProcessRoute
import info.bagen.rust.plaoc.microService.route.webViewRoute
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

data class Origin(val origin: String)


class Gateway(
    val listener: PortListener,
    val host: String,
    val token: String
) {

}

class HttpNMM : NativeMicroModule() {
    override val mmid: String = "http.sys.dweb"
    private val http1Server = Http1Server()

    /// 注册的域名与对应的 token
    val tokenMap = mutableMapOf</* token */ String, Gateway>();
    val gatewayMap = mutableMapOf</* host */ String, Gateway>();

    /**
     * 监听请求
     *
     * 真实过来的请求有两种情况：
     * 1. http://subdomain.localhost:24433
     * 2. http://localhost:24433
     * 前者是桌面端自身 chrome 支持的情况，后者才是常态。
     * 但是我们返回给开发者的端口只有一个，这就意味着我们需要额外手段进行路由
     *
     * 如果这个请求是发生在 nativeFetch 中，我们会将请求的 url 改成 http://localhost:24433，同时在 headers.user-agent 的尾部加上 dweb-host/subdomain.localhost:24433
     * 如果这个请求是 webview 中发出，我们一开始就会为整个 webview 设置 user-agent，使其行为和上条一致
     *
     * 如果在 webview 中，要跨域其它请求，那么 webview 的拦截器能对 get 请求进行简单的转译处理，
     * 否则其它情况下，需要开发者自己用 fetch 接口来发起请求。
     * 这些自定义操作，都需要在 header 中加入 X-Dweb-Host 字段来指明宿主
     */
    private val requestHookPlugin = createApplicationPlugin(name = "RequestHookPlugin") {
        onCall { call ->
            var host = call.request.headers["X-Dweb-Host"]
                ?: call.request.headers["User-Agent"]?.let { user_agent ->
                    Regex("""\sdweb-host/(.+?)\s""").find(user_agent)?.let { matchResult ->
                        matchResult.groupValues[1]
                    }
                } ?: call.request.headers["Host"] ?: "*";
            /// 如果没有端口，补全端口
            if (host.contains(":") === false) {
                host += ":" + Http1Server.PORT;
            }

            /// 在网关中寻址能够处理该 host 的监听者
            val gateway = gatewayMap[host]
                ?: return@onCall call.respond(
                    DefaultErrorResponse(
                        502,
                        "Bad Gateway",
                        "作为网关或者代理工作的服务器尝试执行请求时，从远程服务器接收到了一个无效的响应"
                    )
                );

            /* 30s 没有任何 body 写入的话，认为网关超时 */
            gateway.listener.hookHttpRequest(call.request, call.response)
            call.request.origin.apply {
                println("Request URL: $scheme://$localHost:$localPort$uri")
            }
        }
    }


    public override fun _bootstrap() {
        http1Server.createServer(requestHookPlugin)
    }

    private fun getHost(port: String): String {
        return "http://internal.js.sys.dweb-$port.localhost:${PORT}/js-process";
    }

    public override fun _shutdown() {
        http1Server.closeServer()
    }
}

fun Application.moduleRouter() {
    routing {
        get("/") {
            call.respondText(
                text = "Hello!! You are here in ${Build.MODEL}",
                contentType = ContentType.Text.Plain
            )
        }
        jsProcessRoute()
        webViewRoute()
    }
}

