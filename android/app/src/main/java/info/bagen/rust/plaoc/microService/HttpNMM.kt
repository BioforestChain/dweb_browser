package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.microService.helper.boolean
import info.bagen.rust.plaoc.microService.helper.json
import info.bagen.rust.plaoc.microService.helper.stream
import info.bagen.rust.plaoc.microService.helper.suspendOnce
import info.bagen.rust.plaoc.microService.ipc.IPC_ROLE
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.ReadableStreamIpc
import info.bagen.rust.plaoc.microService.network.Http1Server
import info.bagen.rust.plaoc.microService.network.Http1Server.Companion.PORT
import info.bagen.rust.plaoc.microService.network.PortListener
import info.bagen.rust.plaoc.microService.network.nativeFetch
import org.http4k.core.Filter
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.query


data class Origin(val origin: String)


class Gateway(
    val listener: PortListener,
    val host: String,
    val token: String
)

class HttpNMM() : NativeMicroModule("http.sys.dweb") {
    companion object {
        val DwebServer = Http1Server;
        val dwebServer = Http1Server()
    }


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
    // 创建过滤
    val setContentType = Filter { nextHandler ->
        { request ->
            var host = "*"
            request.headers.forEach { (key, value) ->
                when (key) {
                    "X-Dweb-Host" -> {
                        value?.let {
                            host = it
                        }
                    }
                    "User-Agent" -> {
                        value?.let { user_agent ->
                            Regex("""\sdweb-host/(.+?)\s""").find(user_agent)?.let { matchResult ->
                                println("headers#router User-Agent ====> ${matchResult.groupValues[1]}")
                                host = matchResult.groupValues[1]
                            }
                        }
                    }
                }
            }
            /// 如果没有端口，补全端口
            if (!host.contains(":")) {
                host += ":" + Http1Server.PORT;
            }
            /** 30s 没有任何 body 写入的话，认为网关超时 */
            val gateway = gatewayMap[host]
            val response = nextHandler(request)
            if (gateway == null) {
                println("HttpNMM#gateway111 ===> ${response.body}")
                response.status(Status.BAD_GATEWAY).body("作为网关或者代理工作的服务器尝试执行请求时，从远程服务器接收到了一个无效的响应")
                println("HttpNMM#gateway222 ===> ${response.body}")
            } else {
                gateway.listener.hookHttpRequest(request, response)
            }
            response
        }
    }
    /// 在网关中寻址能够处理该 host 的监听者


    public override suspend fun _bootstrap() {
        // 启动http后端服务
        dwebServer.createServer(setContentType)
    }

    private fun getHost(port: String): String {
        return "http://internal.js.sys.dweb-$port.localhost:${PORT}/js-process";
    }

    public override suspend fun _shutdown() {
        dwebServer.closeServer()
    }
}


/// 对外提供一套建议的操作来创建、使用、维护这个http服务

data class DwebServerOptions(
    val port: Int = HttpNMM.DwebServer.PORT,
    val subdomain: String = ""
)

data class HttpDwebServerInfo(
    val origin: String,
    val token: String
)

suspend fun NativeMicroModule.startHttpDwebServer(options: DwebServerOptions) =
    this.nativeFetch(
        Uri.of("file://http.sys.dweb/start")
            .query("port", options.port.toString())
            .query("subdomain", options.subdomain)
    ).json<HttpDwebServerInfo>(HttpDwebServerInfo::class.java)


suspend fun NativeMicroModule.listenHttpDwebServer(token: String) =
    ReadableStreamIpc(this, IPC_ROLE.CLIENT).also {
        it.bindIncomeStream(
            this.nativeFetch(
                Uri.of("file://http.sys.dweb/listen")
                    .query("token", token)
            ).stream()
        )
    }


suspend fun NativeMicroModule.closeHttpDwebServer(options: DwebServerOptions) =
    this.nativeFetch(
        Uri.of("file://http.sys.dweb/close")
            .query("port", options.port.toString())
            .query("subdomain", options.subdomain)
    ).boolean()

class HttpDwebServer(private val nmm: NativeMicroModule, private val options: DwebServerOptions) {
    val start = suspendOnce { nmm.startHttpDwebServer(options) }
    val listen = suspend { nmm.listenHttpDwebServer(start().token) }
    val close = suspendOnce { nmm.closeHttpDwebServer(options) }
}

suspend fun NativeMicroModule.createHttpDwebServer(options: DwebServerOptions) =
    HttpDwebServer(this, options).also { it.start() }

