package info.bagen.rust.plaoc.microService.sys.http

import com.google.gson.reflect.TypeToken
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.helper.toBase64Url
import info.bagen.rust.plaoc.microService.ipc.IPC_ROLE
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.ReadableStreamIpc
import info.bagen.rust.plaoc.microService.sys.http.net.Http1Server
import info.bagen.rust.plaoc.microService.sys.http.net.PortListener
import info.bagen.rust.plaoc.microService.sys.http.net.RouteConfig
import kotlinx.coroutines.runBlocking
import org.http4k.core.*
import org.http4k.lens.Query
import org.http4k.lens.composite
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.util.*


class Gateway(
    val listener: PortListener, val urlInfo: HttpNMM.ServerUrlInfo, val token: String
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
    val httpHandler: HttpHandler = { request ->
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
        /// TODO 30s 没有任何 body 写入的话，认为网关超时 ß
        gatewayMap[host]?.let { gateway -> runBlocking { gateway.listener.hookHttpRequest(request) } }
            ?: Response(
                Status.NOT_FOUND
            )
    }
    /// 在网关中寻址能够处理该 host 的监听者


    public override suspend fun _bootstrap() {
        // 启动http后端服务
        dwebServer.createServer(httpHandler)

        val query_dwebServerOptions = Query.composite {
            DwebServerOptions(
                port = int().optional("port")(it),
                subdomain = string().optional("subdomain")(it),
            )
        }

        val query_token = Query.string().required("token")
        val query_routeConfig = Query.string().required("routes")
        val type_routes = object : TypeToken<ArrayList<RouteConfig>>() {}.type

        apiRouting = routes(
            "/start" bind Method.GET to defineHandler { request, ipc ->
                start(ipc, query_dwebServerOptions(request))
            },
            "/listen" bind Method.POST to defineHandler { request ->
                val token = query_token(request)
                val routes: List<RouteConfig> =
                    gson.fromJson(query_routeConfig(request), type_routes)
                listen(token, request, routes)
            },
            "/close" bind Method.GET to defineHandler { request, ipc ->
                close(ipc, query_dwebServerOptions(request))
            }
        )
    }

    inner class ServerUrlInfo(val host: String, val origin: String)

    private fun getServerUrlInfo(ipc: Ipc, options: DwebServerOptions): ServerUrlInfo {
        val mmid = ipc.remote.mmid
        val subdomainPrefix =
            if (options.subdomain == "" || options.subdomain.endsWith(".")) options.subdomain else "${options.subdomain}."
        val port = if (options.port <= 0 || options.port >= 65536) DwebServer.PORT else options.port
        val host = "$subdomainPrefix$mmid-$port.localhost:${dwebServer.bindingPort}"
        val origin = "${DwebServer.PREFIX}$host"
        return ServerUrlInfo(host, origin)
    }

    public override suspend fun _shutdown() {
        dwebServer.closeServer()
    }

    data class ServerStartResult(val token: String, val origin: String)

    /**
     * 监听端口，启动服务
     */
    private fun start(ipc: Ipc, options: DwebServerOptions): ServerStartResult {
        val serverUrlInfo = getServerUrlInfo(ipc, options)
        if (gatewayMap.contains(serverUrlInfo.host)) throw Exception("already in listen: ${serverUrlInfo.origin}")

        val listener = PortListener(ipc, serverUrlInfo.host, serverUrlInfo.origin)

        /// ipc 在关闭的时候，自动释放所有的绑定
        listener.onDestroy(ipc.onClose { close(ipc, options) })

        val token = ByteArray(64).also { Random().nextBytes(it) }.toBase64Url()

        val gateway = Gateway(listener, serverUrlInfo, token)
        gatewayMap[serverUrlInfo.host] = gateway
        tokenMap[token] = gateway

        return ServerStartResult(token, serverUrlInfo.origin)
    }

    /**
     *
     */
    private fun listen(
        token: String,
        message: Request,
        routes: List<RouteConfig>
    ): Response {
        val gateway = tokenMap[token] ?: throw Exception("no gateway with token: $token")

        val streamIpc = ReadableStreamIpc(
            gateway.listener.ipc.remote,
            IPC_ROLE.CLIENT
        )
        streamIpc.bindIncomeStream(message.body.stream)
        for (routeConfig in routes) {
            streamIpc.onClose(gateway.listener.addRouter(routeConfig, streamIpc))
        }

        return Response(Status.OK).body(streamIpc.stream)
    }

    private suspend fun close(ipc: Ipc, options: DwebServerOptions) {
        val serverUrlInfo = getServerUrlInfo(ipc, options)
        gatewayMap.remove(serverUrlInfo.host)?.let { gateway ->
            tokenMap.remove(gateway.token)
            gateway.listener.destroy()
        }
    }

}

