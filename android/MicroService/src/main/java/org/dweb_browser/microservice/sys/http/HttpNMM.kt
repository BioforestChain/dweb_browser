package org.dweb_browser.microservice.sys.http

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.decodeURIComponent
import org.dweb_browser.helper.encodeURI
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.removeWhen
import org.dweb_browser.helper.toBase64Url
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.ReadableStreamIpc
import org.dweb_browser.microservice.sys.dns.debugFetch
import org.dweb_browser.microservice.sys.dns.nativeFetchAdaptersManager
import org.dweb_browser.microservice.sys.http.net.Http1Server
import org.http4k.base64Decoded
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.lens.Query
import org.http4k.lens.composite
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.util.Random
import java.util.concurrent.ConcurrentHashMap

fun debugHttp(tag: String, msg: Any = "", err: Throwable? = null) =
  printDebug("http", tag, msg, err)


class HttpNMM : NativeMicroModule("http.std.dweb", "HTTP Server Provider") {
  init {
    short_name = "HTTP"
    categories =
      mutableListOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Protocol_Service)
  }

  companion object {
    val dwebServer = Http1Server()
  }

  /// 注册的域名与对应的 token
  private val tokenMap = ConcurrentHashMap</* token */ String, Gateway>();
  private val gatewayMap = ConcurrentHashMap</* host */ String, Gateway>();

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
  private suspend fun httpHandler(request: Request): Response {
    val host = findRequestGateway(request) ?: return noGatewayResponse

    /// TODO 这里提取完数据后，应该把header、query、uri重新整理一下组成一个新的request会比较好些
    /// TODO 30s 没有任何 body 写入的话，认为网关超时

    debugHttp("httpHandler start", request.uri)
    /**
     * WARNING 我们底层使用 KtorCIO，它是完全以流的形式来将response的内容传输给web
     * 所以这里要小心，不要去读取 response 对象，否则 pos 会被偏移
     */
    val response = gatewayMap[host]?.let { gateway ->
      gateway.listener.hookHttpRequest(request)
    }
    debugHttp("httpHandler end", request.uri)

    return response ?: Response(Status.NOT_FOUND)
  }

  private val noGatewayResponse =
    Response(Status.UNAUTHORIZED).header("WWW-Authenticate", """Basic realm="dweb"""")

  public override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    /// 启动http后端服务
    dwebServer.createServer({ request ->
      findRequestGateway(request)?.let {
        gatewayMap[it]
      }
    }, { gateway, request ->
      gateway.listener.hookHttpRequest(request)
    }, { request, gateway ->
      if (gateway == null) {
        if (request.uri.path == "/debug") {
//            RequestResponseBuilder()
//            kotlinx.coroutines.debug.DebugProbes.dumpCoroutines()
          Response(Status.OK).body(
            request.headers.toString()
          )
        } else noGatewayResponse
      } else Response(Status.NOT_FOUND)
    })

    /// 为 nativeFetch 函数提供支持
    nativeFetchAdaptersManager.append { fromMM, request ->
      if ((request.uri.scheme == "http" || request.uri.scheme == "https") && request.uri.host.endsWith(
          ".dweb"
        )
      ) {
        debugFetch("HTTP/nativeFetch", "$fromMM => ${request.uri}")
        // 无需走网络层，直接内部处理掉
        httpHandler(
          request
            // 头部里添加 X-Dweb-Host
            .header("X-Dweb-Host", request.uri.getFullAuthority())
            // 替换 url 的 authority（host+port）
//            .uri(request.uri.scheme("http").authority(dwebServer.authority))
        )
      } else null
    }.removeWhen(onAfterShutdown);

    /// 模块 API 接口
    val query_dwebServerOptions = Query.composite {
      DwebHttpServerOptions(
        port = int().optional("port")(it),
        subdomain = string().optional("subdomain")(it),
      )
    }
    val query_token = Query.string().required("token")
    val query_routeConfig = Query.string().required("routes")

    apiRouting = routes("/start" bind Method.GET to defineHandler { request, ipc ->
      start(ipc, query_dwebServerOptions(request))
    }, "/listen" bind Method.POST to defineHandler { request ->
      val token = query_token(request)
      val routes = Json.decodeFromString<List<Gateway.RouteConfig>>(query_routeConfig(request))
      listen(token, request, routes)
    }, "/close" bind Method.GET to defineHandler { request, ipc ->
      close(ipc, query_dwebServerOptions(request))
    })
  }

  @Serializable
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
    fun buildPublicUrl() = Uri.of(public_origin).query("X-Dweb-Host", host)
    fun buildPublicHtmlUrl() = Uri.of(public_origin).userInfo(host.encodeURI())

    fun buildInternalUrl() = Uri.of(internal_origin)
  }

  private fun getServerUrlInfo(ipc: Ipc, options: DwebHttpServerOptions): ServerUrlInfo {
    val mmid = ipc.remote.mmid
    val subdomainPrefix =
      if (options.subdomain == "" || options.subdomain.endsWith(".")) options.subdomain else "${options.subdomain}."
    val port =
      if (options.port <= 0 || options.port >= 65536) throw Exception("invalid dweb http port: ${options.port}")
      else options.port
    val host = "$subdomainPrefix$mmid:$port"
    val internal_origin = "https://$host"
    val public_origin = dwebServer.origin
    return ServerUrlInfo(host, internal_origin, public_origin)
  }

  public override suspend fun _shutdown() {
    dwebServer.closeServer()
  }

  @Serializable
  data class ServerStartResult(val token: String, val urlInfo: ServerUrlInfo)

  /**
   * 监听端口，启动服务
   */
  private fun start(ipc: Ipc, options: DwebHttpServerOptions): ServerStartResult {
    val serverUrlInfo = getServerUrlInfo(ipc, options)
    debugHttp("start", "$serverUrlInfo => $options")
    if (gatewayMap.contains(serverUrlInfo.host)) throw Exception("already in listen: ${serverUrlInfo.internal_origin}")
    val listener = Gateway.PortListener(ipc, serverUrlInfo.host)

    listener.onDestroy {
      close(ipc, options)
    }
    /// ipc 在关闭的时候，自动释放所有的绑定
    ipc.onClose {
      debugHttp("start close", "onDestroy ${ipc.remote.mmid} ${serverUrlInfo.host}")
      listener.destroy()
    }
    val token = ByteArray(8).also { Random().nextBytes(it) }.toBase64Url()

    val gateway = Gateway(listener, serverUrlInfo, token)
    gatewayMap[serverUrlInfo.host] = gateway
    tokenMap[token] = gateway
    return ServerStartResult(token, serverUrlInfo)
  }

  /**
   *  绑定流监听
   */
  private fun listen(
    token: String, message: Request, routes: List<Gateway.RouteConfig>
  ): Response {
    debugHttp("LISTEN", tokenMap.keys.toList())
    val gateway = tokenMap[token] ?: throw Exception("no gateway with token: $token")
    debugHttp("LISTEN", "host: ${gateway.urlInfo.host}, token: $token")

    val streamIpc = ReadableStreamIpc(
      gateway.listener.ipc.remote, "http-gateway/${gateway.urlInfo.host}"
    )
    /// 接收一个body，body在关闭的时候，fetchIpc也会一同关闭
    streamIpc.bindIncomeStream(message.body.stream)
    /// 自己nmm销毁的时候，ipc也会被全部销毁
    this.addToIpcSet(streamIpc)
    /// 自己创建的，就要自己销毁：这个listener被销毁的时候，streamIpc也要进行销毁
    gateway.listener.onDestroy {
      streamIpc.close()
    }
    for (routeConfig in routes) {
      gateway.listener.addRouter(routeConfig, streamIpc).removeWhen(streamIpc.onClose)
    }

    return Response(Status.OK).body(streamIpc.stream)
  }

  private suspend fun close(ipc: Ipc, options: DwebHttpServerOptions): Boolean {
    val serverUrlInfo = getServerUrlInfo(ipc, options)
    return gatewayMap.remove(serverUrlInfo.host)?.let { gateway ->
      debugHttp("close", "mmid: ${ipc.remote.mmid} ${serverUrlInfo.host}")
      tokenMap.remove(gateway.token)
      gateway.listener.destroy()
      true
    } ?: false
  }
}

fun findRequestGateway(request: Request): String? {
  var header_host: String? = null
  var header_x_dweb_host: String? = null
  var header_auth_host: String? = null
  val query_x_dweb_host: String? = request.query("X-Dweb-Host")?.decodeURIComponent()
  for ((key, value) in request.headers) {
    when (key) {
      "Host" -> {
        if (value != null && Regex("""\.dweb(:\d+)?$""").matches(value))
          header_host = value
      }

      "X-Dweb-Host" -> {
        header_x_dweb_host = value
      }

      "Authorization" -> {
        if (value != null) {
          Regex("""^ *(?:[Bb][Aa][Ss][Ii][Cc]) +([A-Za-z0-9._~+/-]+=*) *$""").find(value)
            ?.also { matchResult ->
              matchResult.groupValues.getOrNull(1)?.also { base64Content ->
                val userInfo = base64Content.base64Decoded()
                val splitIndex = userInfo.lastIndexOf(':')
                header_auth_host = if (splitIndex == -1) {
                  userInfo
                } else {
                  userInfo.slice(0 until splitIndex)
                }.decodeURIComponent()
              }
            }
        }
      }
    }
  }
  val x_dweb_host = query_x_dweb_host ?: header_auth_host ?: header_x_dweb_host
  ?: header_host
  return x_dweb_host?.let { host ->
    /// 如果没有端口，补全端口
    if (!host.contains(":")) {
      host + ":" + Http1Server.PORT;
    } else host
  }
}

fun Uri.getFullAuthority(hostOrAuthority: String = authority): String {
  var authority1 = hostOrAuthority
  if (!authority1.contains(":")) {
    if (scheme == "http") {
      authority1 += ":80"
    } else if (scheme == "https") {
      authority1 += ":443"
    }
  }
  return authority1
}