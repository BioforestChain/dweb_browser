package info.bagen.rust.plaoc.microService.sys.http

import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.sys.http.net.Http1Server
import info.bagen.rust.plaoc.microService.sys.http.net.PortListener
import org.http4k.core.*
import org.http4k.lens.*
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes


data class Origin(val origin: String)


class Gateway(
    val listener: RoutingHttpHandler,
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
                gateway.listener(request)
            }
            response
        }
    }
    /// 在网关中寻址能够处理该 host 的监听者


    public override suspend fun _bootstrap() {
        // 启动http后端服务
        dwebServer.createServer(setContentType)

        val query_DwebServerOptions = Query.composite {
            DwebServerOptions(
                port = int().optional("port")(it),
                subdomain = string().optional("subdomain")(it),
            )
        }

        apiRouting = routes(
            "/start" bind Method.GET to defineHandler { request, ipc -> }
        )
    }

    inner class HostInfo(val host: String, val origin: String)

    private fun getHost(ipc: Ipc, options: DwebServerOptions): HostInfo {
        val mmid = ipc.remote.mmid
        val subdomainPrefix =
            if (options.subdomain == "" || options.subdomain.endsWith(".")) options.subdomain else "${options.subdomain}."
        val port = if (options.port <= 0 || options.port >= 65536) DwebServer.PORT else options.port
        val host = "$subdomainPrefix$mmid-$port.localhost:${dwebServer.bindingPort}"
        val origin = "${DwebServer.PREFIX}$host"
        return HostInfo(host, origin)
    }

    public override suspend fun _shutdown() {
        dwebServer.closeServer()
    }

    private fun start(ipc: Ipc, options: DwebServerOptions) {
        val hostInfo = getHost(ipc, options)
        if (gatewayMap.contains(hostInfo.host))
            throw  Exception("already in listen: ${hostInfo.origin}")

        val listener = PortListener(ipc,hostInfo.host,hostInfo.origin)
    }

}

