package info.bagen.rust.plaoc.microService.sys.http

import com.google.gson.reflect.TypeToken
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.helper.toBase64Url
import info.bagen.rust.plaoc.microService.ipc.IPC_ROLE
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.ReadableStreamIpc
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetchAdaptersManager
import info.bagen.rust.plaoc.microService.sys.dns.networkFetch
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
        val dwebServer = Http1Server()
    }


    /// 注册的域名与对应的 token
    private val tokenMap = mutableMapOf</* token */ String, Gateway>();
    private val gatewayMap = mutableMapOf</* host */ String, Gateway>();

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
    private val httpHandler: HttpHandler = { request ->
        var header_host: String? = null
        var header_x_dweb_host: String? = null
        var header_user_agent_host: String? = null
        var query_x_web_host: String? = request.query("X-DWeb-Host")
        for ((key, value) in request.headers) {
            when (key) {
                "Host" -> {
                    header_host = value
                }
                "X-Dweb-Host" -> {
                    header_x_dweb_host = value
                }
                "User-Agent" -> {
                    if (value != null) {
                        Regex("""\sdweb-host/(.+)\s*""").find(value)?.also { matchResult ->
                            header_user_agent_host = matchResult.groupValues[1]
                        }
                    }
                }
            }
        }
        val host = (
                query_x_web_host ?: header_x_dweb_host ?: header_user_agent_host
                ?: header_host)?.let { host ->
            /// 如果没有端口，补全端口
            if (!host.contains(":")) {
                host + ":" + Http1Server.PORT;
            } else host
        } ?: "*"

        /// TODO 这里提取完数据后，应该把header、query、uri重新整理一下组成一个新的request会比较好些
        /// TODO 30s 没有任何 body 写入的话，认为网关超时

        /**
         * WARNING 我们底层使用 KtorCIO，它是完全以流的形式来将response的内容传输给web
         * 所以这里要小心，不要去读取 response 对象，否则 pos 会被偏移
         */
        val response = gatewayMap[host]?.let { gateway ->
            println("URL:${request.uri} => gateway: ${gateway.urlInfo}")
            runBlocking {
                val response = gateway.listener.hookHttpRequest(request)
//                println("URL:${request.uri} => response: $response")
                response
            }
        }

        response ?: Response(
            Status.NOT_FOUND
        )
    }
    /// 在网关中寻址能够处理该 host 的监听者


    public override suspend fun _bootstrap() {
        /// 启动http后端服务
        dwebServer.createServer(httpHandler)

        /// 为 nativeFetch 函数提供支持
        _afterShutdownSignal.listen(nativeFetchAdaptersManager.append { _, request ->
            if (request.uri.scheme == "http" && request.uri.host.endsWith(".dweb")) {
                networkFetch(
                    request
                        // 头部里添加 X-Dweb-Host
                        .header("X-Dweb-Host", request.uri.authority)
                        // 替换 url 的 authority（host+port）
                        .uri(request.uri.authority(dwebServer.authority))
                )
            } else null
        });

        /// 模块 API 接口
        val query_dwebServerOptions = Query.composite {
            DwebHttpServerOptions(
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

    data class ServerUrlInfo(
        /**
         * 标准host，是一个站点的key，只要站点过来时用某种我们认可的方式（x-host/user-agent）携带了这个信息，那么我们就依次作为进行网关路由
         */
        val host: String,
        /**
         * 内部链接，带有特殊的协议头，方便自定义解析器对其进行加工
         */
        val internal_origin: String,
        /**
         * 相对公网的链接（这里只是相对标准网络访问，当然目前本地只支持localhost链接，所以这里只是针对webview来使用）
         */
        val public_origin: String,
    ) {
        fun buildHttpUrl() = Uri.of(public_origin)
            .query("X-DWeb-Host", host)
    }

    private fun getServerUrlInfo(ipc: Ipc, options: DwebHttpServerOptions): ServerUrlInfo {
        val mmid = ipc.remote.mmid
        val subdomainPrefix =
            if (options.subdomain == "" || options.subdomain.endsWith(".")) options.subdomain else "${options.subdomain}."
        val port =
            if (options.port <= 0 || options.port >= 65536) throw Exception("invalid dweb http port: ${options.port}")
            else options.port
        val host = "$subdomainPrefix$mmid:$port"
        val internal_origin = "http://$host"
        val public_origin = dwebServer.origin
        return ServerUrlInfo(host, internal_origin, public_origin)
    }

    public override suspend fun _shutdown() {
        dwebServer.closeServer()
    }

    data class ServerStartResult(val token: String, val urlInfo: ServerUrlInfo)

    /**
     * 监听端口，启动服务
     */
    private fun start(ipc: Ipc, options: DwebHttpServerOptions): ServerStartResult {
        val serverUrlInfo = getServerUrlInfo(ipc, options)
        if (gatewayMap.contains(serverUrlInfo.host)) throw Exception("already in listen: ${serverUrlInfo.internal_origin}")

        val listener = PortListener(ipc, serverUrlInfo.host)

        /// ipc 在关闭的时候，自动释放所有的绑定
        listener.onDestroy(ipc.onClose { close(ipc, options) })

        val token = ByteArray(64).also { Random().nextBytes(it) }.toBase64Url()

        val gateway = Gateway(listener, serverUrlInfo, token)
        gatewayMap[serverUrlInfo.host] = gateway
        tokenMap[token] = gateway

        return ServerStartResult(token, serverUrlInfo)
    }

    /**
     *  绑定流监听
     */
    private fun listen(
        token: String,
        message: Request,
        routes: List<RouteConfig>
    ): Response {
        val gateway = tokenMap[token] ?: throw Exception("no gateway with token: $token")

        val streamIpc = ReadableStreamIpc(
            gateway.listener.ipc.remote,
            IPC_ROLE.SERVER
        )
        streamIpc.bindIncomeStream(message.body.stream, "http-gateway")
        for (routeConfig in routes) {
            streamIpc.onClose(gateway.listener.addRouter(routeConfig, streamIpc))
        }

        return Response(Status.OK).body(streamIpc.stream)
    }

    private suspend fun close(ipc: Ipc, options: DwebHttpServerOptions) {
        val serverUrlInfo = getServerUrlInfo(ipc, options)
        gatewayMap.remove(serverUrlInfo.host)?.let { gateway ->
            tokenMap.remove(gateway.token)
            gateway.listener.destroy()
        }
    }

}

