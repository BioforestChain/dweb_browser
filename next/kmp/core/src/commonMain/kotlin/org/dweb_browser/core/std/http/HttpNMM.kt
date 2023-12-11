package org.dweb_browser.core.std.http

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.http.protocolWithAuthority
import io.ktor.util.decodeBase64String
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.isWebSocket
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.IPureBody
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.PureStream
import org.dweb_browser.core.http.PureStringBody
import org.dweb_browser.core.http.queryAs
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.by
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.ReadableStreamIpc
import org.dweb_browser.core.ipc.helper.IpcHeaders
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.debugFetch
import org.dweb_browser.core.std.dns.httpFetch
import org.dweb_browser.core.std.dns.nativeFetchAdaptersManager
import org.dweb_browser.core.std.http.HttpNMM.Companion.dwebServer
import org.dweb_browser.core.std.http.net.Http1Server
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.decodeURIComponent
import org.dweb_browser.helper.encodeURI
import org.dweb_browser.helper.falseAlso
import org.dweb_browser.helper.removeWhen
import org.dweb_browser.helper.toBase64Url
import org.dweb_browser.helper.toJsonElement
import kotlin.random.Random

val debugHttp = Debugger("http")


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
    val info = findDwebGateway(request) ?: return noGatewayResponse

    /// TODO 这里提取完数据后，应该把header、query、uri重新整理一下组成一个新的request会比较好些
    /// TODO 30s 没有任何 body 写入的话，认为网关超时

    debugHttp("httpHandler start", request.href)
    /**
     * WARNING 我们底层使用 KtorCIO，它是完全以流的形式来将response的内容传输给web
     * 所以这里要小心，不要去读取 response 对象，否则 pos 会被偏移
     */
    val response = gatewayMap[info.host]?.listener?.hookHttpRequest(request)
    debugHttp("httpHandler end", request.href)

    return response ?: PureResponse(HttpStatusCode.NotFound)
  }

  private val noGatewayResponse
    get() = PureResponse(
      HttpStatusCode.BadGateway,
      IpcHeaders(
        mutableMapOf(
          "Content-Type" to "text/html"
        )
      ),
      IPureBody.from("no found gateway")
    )

  public override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    /// 启动http后端服务
    dwebServer.createServer(
      gatewayHandler = { request ->
        findDwebGateway(request)?.let { info ->
          gatewayMap[info.host]
        }
      },
      httpHandler = { gateway, request ->
        gateway.listener.hookHttpRequest(request)
      },
      errorHandler = { request, gateway ->
        if (gateway == null) {
          if (request.url.fullPath == "/debug") {
            PureResponse(HttpStatusCode.OK, body = PureStringBody(request.headers.toString()))
          } else noGatewayResponse
        } else PureResponse(HttpStatusCode.NotFound)
      })

    /// 为 nativeFetch 函数提供支持
    nativeFetchAdaptersManager.append { fromMM, request ->
      if ((request.url.protocol.name == "http" || request.url.protocol.name == "https") && request.url.host.endsWith(
          ".dweb"
        )
      ) {
        debugFetch(
          "HTTP/nativeFetch", "$fromMM => ${request.href} authority-> ${dwebServer.authority}"
        )
        // 头部里添加 X-Dweb-Host
        request.headers.set("X-Dweb-Host", request.url.run { "$host:$port" })
        // 无需走网络层，直接内部处理掉
        httpHandler(request)
      } else null
    }.removeWhen(onAfterShutdown)

    /// 模块 API 接口
    routes(
      // 开启一个服务
      "/start" bind HttpMethod.Get by defineJsonResponse {
        start(
          ipc, DwebHttpServerOptions(request.query("subdomain"))
        ).toJsonElement()
      },
      // 监听一个服务
      "/listen" bind HttpMethod.Post by definePureStreamHandler {
        val token = request.query("token")
        val routes = Json.decodeFromString<List<CommonRoute>>(request.query("routes"))
        listen(token, request, routes)
      },
      // 主动关闭一个服务
      "/close" bind HttpMethod.Get by defineBooleanResponse {
        close(ipc, request.queryAs())
      },
      "/websocket" by definePureResponse {
        TODO("")
      },
      "/fetch" by definePureResponse {
        if (request.url.protocol != URLProtocol.HTTP || request.url.protocol != URLProtocol.HTTPS) {
          throwException(
            HttpStatusCode.BadRequest,
            "invalid request protocol: ${request.url.protocol.name}"
          )
        }
        /// 如果是options类型的请求，直接放行，不做任何同域验证
        if (request.method == IpcMethod.OPTIONS) {
          return@definePureResponse httpFetch(request)
        }
        val isSameOrigin =
          request.url.host.let { host -> host == ipc.remote.mmid || host.endsWith(".${ipc.remote.mmid}") }
        /// 否则如果域名，那么才能直接放行
        if (isSameOrigin) {
          return@definePureResponse httpFetch(request)
        }
        /// 如果不是同域，需要进行跨域判定

        // 首先根据标准，判断是否需要进行 options 请求请求options获取 allow-method
        val needPreflightRequest = when (request.method) {
          IpcMethod.GET, IpcMethod.POST, IpcMethod.HEAD -> {
            var isSimple = true
            for ((key, value) in request.headers) {
              if (key in preflightRequestHeaderKeys) {
                isSimple = false
                break
              }
              if (!isSimpleHeader(key, value)) {
                isSimple = false
                break
              }
            }
            isSimple
          }

          else -> false
        }

        /**
         * 如果需要发起“预检请求”，那么根据预检请求返回一个专门用于 cors 的 request 对象
         */
        val corsRequest = if (needPreflightRequest) {
          val optionsResponse = httpFetch(request.href, IpcMethod.OPTIONS)

          val get = optionsResponse.headers::get;
          val allowOrigin = get(HttpHeaders.AccessControlAllowOrigin)
          val allowMethods = get(HttpHeaders.AccessControlAllowMethods)
          val allowHeaders by lazy {
            get(HttpHeaders.AccessControlAllowHeaders)?.split(',')?.toSet() ?: setOf()
          }
          // TODO 缓存
          // val maxAge = get(HttpHeaders.AccessControlMaxAge)

          when (allowOrigin) {
            null -> false
            "*" -> true
            else -> allowOrigin.split(",").find { it == request.url.protocolWithAuthority } == null
          }.falseAlso {
            throwException(HttpStatusCode.NotAcceptable, "no-cors by origin")
          }

          when (allowMethods) {
            null -> when (request.method) {
              IpcMethod.GET, IpcMethod.POST, IpcMethod.HEAD -> true;
              else -> false
            }

            "*" -> true
            else -> request.method.method in allowMethods.split(',')
          }.falseAlso {
            throwException(HttpStatusCode.NotAcceptable, "no-cors by method")
          }


          PureRequest(request.href, request.method, IpcHeaders(request.headers.toList().filter {
            if (isSimpleHeader(it.first, it.second))
              true
            else it.first in allowHeaders
          }), body = request.body, from = request.from)
        } else {
          request
        }
        // 正式发起请求
        val corsResponse = httpFetch(corsRequest)
        if (corsRequest.headers.has("Cookie") || corsRequest.headers.has("Authorization") && corsResponse.headers.get(
            "HttpHeaders.AccessControlAllowCredentials"
          ) != "true"
        ) {
          throwException(HttpStatusCode.NoContent)
        }
        /// AccessControlExposeHeaders 默认不需要工作
        return@definePureResponse corsResponse
      }
    );
  }

  private val simpleRequestHeaderKeys = setOf(
    "Accept",
    "Accept-Encoding",
    "Accept-Language",
    "Access-Control-Request-Headers",
    "Access-Control-Request-Method",
    "Connection",
    "Content-Length",
    "Content-Type",
    "Range",
    "Cookie",
    "Cookie2",
    "Date",
    "DNT",
    "Expect",
    "Host",
    "Keep-Alive",
    "Origin",
    "Referer",
    "Set-Cookie",
    "TE",
    "Trailer",
    "Transfer-Encoding",
    "Upgrade",
    "Via",
    "ETag",
    "Downlink",
    "Save-Data",
    "Viewport-Width",
    "Width",
    "DPR",
    "User-Agent",
    /// 这里不支持 X-* 字段，所以不考虑
  )

  private val preflightRequestHeaderKeys = setOf(
    "Access-Control-Request-Headers",
    "Access-Control-Request-Method",
  )

  private val simpleRequestContentType = setOf(
    "application/x-www-form-urlencoded", "multipart/form-data", "text/plain"
  )

  private fun isSimpleHeader(key: String, value: String): Boolean {
    if (key == "Content-Type" && value.split(";", limit = 2)
        .first() !in simpleRequestContentType
    ) {
      return false
    }
    if (key.startsWith("sec-", ignoreCase = true) || key.startsWith("proxy-", ignoreCase = true)) {
      return true
    }
    return key in simpleRequestHeaderKeys
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

    fun buildInternalUrl(builder: (URLBuilder.() -> Unit)? = null): Url {
      return when (builder) {
        null -> Url(internal_origin)
        else -> URLBuilder(internal_origin).run { builder();build() };
      }
    }
  }

  private fun getServerUrlInfo(ipc: Ipc, options: DwebHttpServerOptions): ServerUrlInfo {
    val mmid = ipc.remote.mmid
    val subdomainPrefix =
      if (options.subdomain == "" || options.subdomain.endsWith(".")) options.subdomain else "${options.subdomain}."
    val host = "$subdomainPrefix$mmid"
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
    debugHttp("START/end", "$serverUrlInfo => $options")
    return ServerStartResult(token, serverUrlInfo)
  }

  /**
   *  绑定流监听
   */
  private fun listen(
    token: String, message: PureRequest, routes: List<CommonRoute>
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
val reg_referer = Regex("Referer", RegexOption.IGNORE_CASE)
val reg_x_dweb_host = Regex("X-Dweb-Host", RegexOption.IGNORE_CASE)
val reg_authorization = Regex("Authorization", RegexOption.IGNORE_CASE)

data class DwebGatewayInfo(val host: String, val protocol: URLProtocol)

fun findDwebGateway(request: PureRequest): DwebGatewayInfo? {
  if (request.url.host.endsWith(".dweb")) {
    return DwebGatewayInfo(host = request.url.host, protocol = request.url.protocol)
  }
  var header_host: String? = null
  var header_x_dweb_host: String? = null
  var header_auth_host: String? = null
  val query_x_dweb_host: String? = request.queryOrNull("X-Dweb-Host")?.decodeURIComponent()
  var is_https = false
  for ((key, value) in request.headers) {
    if (reg_host.matches(key)) {
      // 解析subDomain
      header_host = if (value.endsWith(".dweb")) {
        value
      } else if (value.endsWith(".${dwebServer.authority}")) {
        value.substring(0, value.length - dwebServer.authority.length - 1)
      } else null
    } else if (reg_referer.matches(key)) {
      is_https = value.startsWith("https://")
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
    val isWs = request.isWebSocket()
    val protocol = when (is_https) {
      true -> if (isWs) URLProtocol.WSS else URLProtocol.HTTPS
      false -> if (isWs) URLProtocol.WS else URLProtocol.HTTP
    }
    DwebGatewayInfo(host, protocol)
  }
}
