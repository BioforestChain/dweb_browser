package org.dweb_browser.microservice.sys.http

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.ParametersBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.authority
import io.ktor.http.fullPath
import io.ktor.util.decodeBase64String
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.decodeURIComponent
import org.dweb_browser.helper.encodeURI
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.removeWhen
import org.dweb_browser.helper.toBase64Url
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.http.PureRequest
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.http.PureStream
import org.dweb_browser.microservice.http.PureStringBody
import org.dweb_browser.microservice.http.bind
import org.dweb_browser.microservice.http.routes
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.ReadableStreamIpc
import org.dweb_browser.microservice.ipc.helper.IpcHeaders
import org.dweb_browser.microservice.sys.dns.debugFetch
import org.dweb_browser.microservice.sys.dns.nativeFetchAdaptersManager
import org.dweb_browser.microservice.sys.http.net.Http1Server
import kotlin.random.Random

fun debugHttp(tag: String, msg: Any = "", err: Throwable? = null) =
  printDebug("http", tag, msg, err)


class HttpNMM : NativeMicroModule("http.std.dweb", "HTTP Server Provider") {
  init {
    short_name = "HTTP"
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Protocol_Service)
  }

  companion object {
    val dwebServer = Http1Server()
  }

  /// 注册的域名与对应的 token
  private val tokenMap = SafeHashMap</* token */ String, Gateway>();
  private val gatewayMap = SafeHashMap</* host */ String, Gateway>();

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
  private suspend fun httpHandler(request: PureRequest): PureResponse {
    val host = findRequestGateway(request) ?: return noGatewayResponse

    /// TODO 这里提取完数据后，应该把header、query、uri重新整理一下组成一个新的request会比较好些
    /// TODO 30s 没有任何 body 写入的话，认为网关超时

    debugHttp("httpHandler start", request.url)
    /**
     * WARNING 我们底层使用 KtorCIO，它是完全以流的形式来将response的内容传输给web
     * 所以这里要小心，不要去读取 response 对象，否则 pos 会被偏移
     */
    val response = gatewayMap[host]?.let { gateway ->
      gateway.listener.hookHttpRequest(request)
    }
    debugHttp("httpHandler end", request.url)

    return response ?: PureResponse(HttpStatusCode.NotFound)
  }

  private val noGatewayResponse
    get() = PureResponse(
      HttpStatusCode.Unauthorized,
      IpcHeaders(mutableMapOf(Pair("WWW-Authenticate", """Basic realm="dweb"""")))
    )

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
        if (request.safeUrl.fullPath == "/debug") {
          PureResponse(HttpStatusCode.OK, body = PureStringBody(request.headers.toString()))
        } else noGatewayResponse
      } else PureResponse(HttpStatusCode.NotFound)
    })

    /// 为 nativeFetch 函数提供支持
    nativeFetchAdaptersManager.append { fromMM, request ->
      if ((request.safeUrl.protocol.name == "http" || request.safeUrl.protocol.name == "https") && request.safeUrl.host.endsWith(
          ".dweb"
        )
      ) {
        debugFetch("HTTP/nativeFetch", "$fromMM => ${request.url} authority-> ${dwebServer.authority}")
        // 头部里添加 X-Dweb-Host
        request.headers.set("X-Dweb-Host", request.safeUrl.authority)
        // 无需走网络层，直接内部处理掉
        // FIXME rebase
        httpHandler(request.copy(url = URLBuilder(request.safeUrl).apply{protocol = URLProtocol.HTTP; authority = dwebServer.authority; buildString()}))
      } else null
    }.removeWhen(onAfterShutdown)

    /// 模块 API 接口
    routes("/start" bind HttpMethod.Get to defineJsonResponse {
      start(
        ipc, DwebHttpServerOptions(
          request.queryOrFail("port").toInt(), request.queryOrFail("subdomain")
        )
      ).toJsonElement()
    },
      //
      "/listen" bind HttpMethod.Post to definePureStreamHandler {
        val token = request.queryOrFail("token")
        val routes = Json.decodeFromString<List<Gateway.RouteConfig>>(request.queryOrFail("routes"))
        listen(token, request, routes)
      },
      //
      "/close" bind HttpMethod.Get to defineBooleanResponse {
        close(ipc, request.queryAsObject())
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
    fun buildPublicUrl(): Url {
      val builder = URLBuilder(public_origin);
//      builder.encodedParameters.caseInsensitiveName = true
      builder.parameters.append("X-Dweb-Host", host)
      return builder.build()
    }

    fun buildPublicHtmlUrl(): Url {
      val builder = URLBuilder(public_origin);
      builder.user = host.encodeURI()
      return builder.build()
    }

    fun buildInternalUrl(): Url {
      return Url(internal_origin);
    }
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
    debugHttp("START/start", "$serverUrlInfo => $options")
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
    val token = ByteArray(8).also { Random.nextBytes(it) }.toBase64Url()

    val gateway = Gateway(listener, serverUrlInfo, token)
    gatewayMap[serverUrlInfo.host] = gateway
    tokenMap[token] = gateway
    debugHttp("START/end","$serverUrlInfo => $options")
    return ServerStartResult(token, serverUrlInfo)
  }

  /**
   *  绑定流监听
   */
  private fun listen(
    token: String, message: PureRequest, routes: List<Gateway.RouteConfig>
  ): PureStream {
    debugHttp("LISTEN", tokenMap.keys.toList())
    val gateway = tokenMap[token] ?: throw Exception("no gateway with token: $token")
    debugHttp("LISTEN/start", "host: ${gateway.urlInfo.host}, token: $token")

    val streamIpc = ReadableStreamIpc(
      gateway.listener.ipc.remote, "http-gateway/${gateway.urlInfo.host}"
    )
    /// 接收一个body，body在关闭的时候，fetchIpc也会一同关闭
    streamIpc.bindIncomeStream(message.body.toPureStream())
    /// 自己nmm销毁的时候，ipc也会被全部销毁
    this.addToIpcSet(streamIpc)
    /// 自己创建的，就要自己销毁：这个listener被销毁的时候，streamIpc也要进行销毁
    gateway.listener.onDestroy {
      streamIpc.close()
    }
    for (routeConfig in routes) {
      gateway.listener.addRouter(routeConfig, streamIpc).removeWhen(streamIpc.onClose)
    }

    return streamIpc.input.stream
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

val reg_host = Regex("Host", RegexOption.IGNORE_CASE)
val reg_x_dweb_host = Regex("X-Dweb-Host", RegexOption.IGNORE_CASE)
val reg_authorization = Regex("Authorization", RegexOption.IGNORE_CASE)
fun findRequestGateway(request: PureRequest): String? {
  var header_host: String? = null
  var header_x_dweb_host: String? = null
  var header_auth_host: String? = null
  val query_x_dweb_host: String? = request.query("X-Dweb-Host")?.decodeURIComponent()
  for ((key, value) in request.headers) {
    if (reg_host.matches(key)) {
      if (Regex("""\.dweb(:\d+)?$""").matches(value)) header_host = value
    } else if (reg_x_dweb_host.matches(key)) {
      header_x_dweb_host = value
    } else if (reg_authorization.matches(key)) {
      Regex("""^ *(?:[Bb][Aa][Ss][Ii][Cc]) +([A-Za-z0-9._~+/-]+=*) *$""").find(value)
        ?.also { matchResult ->
          matchResult.groupValues.getOrNull(1)?.also { base64Content ->
            val userInfo = base64Content.decodeBase64String()
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
  val x_dweb_host = query_x_dweb_host ?: header_auth_host ?: header_x_dweb_host ?: header_host
  return x_dweb_host?.let { host ->
    /// 如果没有端口，补全端口
    if (!host.contains(":")) {
      host + ":" + Http1Server.PORT;
    } else host
  }
}

fun Url.getFullAuthority(hostOrAuthority: String = authority): String {
  var authority1 = hostOrAuthority
  if (!authority1.contains(":")) {
    if (protocol.name == "http") {
      authority1 += ":80"
    } else if (protocol.name == "https") {
      authority1 += ":443"
    }
  }
  return authority1
}