package org.dweb_browser.core.std.http

import io.ktor.client.plugins.websocket.ws
import io.ktor.client.plugins.websocket.wss
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.http.protocolWithAuthority
import io.ktor.util.decodeBase64String
import io.ktor.utils.io.reader
import io.ktor.websocket.Frame
import io.ktor.websocket.FrameType
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.isWebSocket
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.by
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.ReadableStreamIpc
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
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureServerRequest
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.pure.http.PureStreamBody
import org.dweb_browser.pure.http.PureStringBody
import org.dweb_browser.pure.http.queryAs
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
  private suspend fun httpHandler(request: PureClientRequest): PureResponse {
    val info = findDwebGateway(request.toServer()) ?: return noGatewayResponse

    /// TODO 这里提取完数据后，应该把header、query、uri重新整理一下组成一个新的request会比较好些
    /// TODO 30s 没有任何 body 写入的话，认为网关超时

    debugHttp("httpHandler start", request.href)
    /**
     * WARNING 我们底层使用 KtorCIO，它是完全以流的形式来将response的内容传输给web
     * 所以这里要小心，不要去读取 response 对象，否则 pos 会被偏移
     */
    val response = gatewayMap[info.host]?.listener?.hookHttpRequest(request.toServer())
    debugHttp("httpHandler end", request.href)

    return response ?: PureResponse(HttpStatusCode.NotFound)
  }

  private val noGatewayResponse
    get() = PureResponse(
      HttpStatusCode.BadGateway,
      PureHeaders(
        mutableMapOf(
          "Content-Type" to "text/html"
        )
      ),
      IPureBody.from("no found gateway")
    )

  private suspend fun initHttpListener() {
    val options = DwebHttpServerOptions("")
    val selfIpc = connect(mmid)
    val serverUrlInfo = getServerUrlInfo(selfIpc, options)
    val listener = Gateway.PortListener(selfIpc, serverUrlInfo.host)

    listener.onDestroy {
      close(selfIpc, options)
    }
    /// ipc 在关闭的时候，自动释放所有的绑定
    selfIpc.onClose {
      listener.destroy()
    }

    selfIpc.onRequest { (ipcRequest, ipc) ->
      println(ipcRequest.req_id)
    }

    val token = ByteArray(8).also { Random.nextBytes(it) }.toBase64Url()
    val gateway = Gateway(listener, serverUrlInfo, token)
    gatewayMap[serverUrlInfo.host] = gateway
    tokenMap[token] = gateway

    val routes = arrayOf(
      CommonRoute(pathname = "", method = PureMethod.GET),
      CommonRoute(pathname = "", method = PureMethod.POST),
      CommonRoute(pathname = "", method = PureMethod.PUT),
      CommonRoute(pathname = "", method = PureMethod.DELETE),
      CommonRoute(pathname = "", method = PureMethod.OPTIONS),
      CommonRoute(pathname = "", method = PureMethod.PATCH),
      CommonRoute(pathname = "", method = PureMethod.HEAD),
      CommonRoute(pathname = "", method = PureMethod.CONNECT),
      CommonRoute(pathname = "", method = PureMethod.TRACE)
    )

    for (routeConfig in routes) {
      gateway.listener.addRouter(routeConfig, selfIpc).removeWhen(selfIpc.onClose)
    }
  }

  public override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    ioAsyncScope.launch {
      initHttpListener();
    }
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
      if ((request.url.protocol == URLProtocol.HTTP
            || request.url.protocol == URLProtocol.HTTPS
            || request.url.protocol == URLProtocol.WS
            || request.url.protocol == URLProtocol.WSS
            ) && request.url.host.endsWith(
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
      "/start" bind PureMethod.GET by defineJsonResponse {
        start(
          ipc, DwebHttpServerOptions(request.query("subdomain"))
        ).toJsonElement()
      },
      // 监听一个服务
      "/listen" bind PureMethod.POST by definePureStreamHandler {
        val token = request.query("token")
        val routes = Json.decodeFromString<List<CommonRoute>>(request.query("routes"))
        listen(token, request, routes)
      },
      // 主动关闭一个服务
      "/close" bind PureMethod.GET by defineBooleanResponse {
        close(ipc, request.queryAs())
      },
      "/websocket" byChannel { ctx ->
        val rawUrl = request.query("url")
        val url = Url(rawUrl)
        val protocol = URLProtocol.byName[url.protocol.name]

        if (protocol != URLProtocol.WS && protocol != URLProtocol.WSS) {
          throwException(
            HttpStatusCode.BadRequest,
            "invalid request protocol: ${request.url.protocol.name}"
          )
        }

        when (protocol) {
          URLProtocol.WS -> {
            httpFetch.client.ws(rawUrl) {
              reader {
                for (frame in incoming) {
                  when (frame.frameType) {
                    FrameType.TEXT -> {
                      ctx.sendText((frame as Frame.Text).readText())
                    }

                    FrameType.BINARY -> {
                      ctx.sendBinary((frame as Frame.Binary).readBytes())
                    }

                    else -> {}
                  }
                }
              }
            }
          }

          URLProtocol.WSS -> {
            httpFetch.client.wss(rawUrl) {
              reader {
                for (frame in incoming) {
                  when (frame.frameType) {
                    FrameType.TEXT -> {
                      ctx.sendText((frame as Frame.Text).readText())
                    }

                    FrameType.BINARY -> {
                      ctx.sendBinary((frame as Frame.Binary).readBytes())
                    }

                    else -> {}
                  }
                }
              }
            }
          }
        }
      },
      "/fetch" by definePureResponse {
        val url = Url(request.query("url"))
        if (request.method == PureMethod.OPTIONS) {
          val requestOrigin = request.headers.get(HttpHeaders.Origin)
          val requestMethod = request.headers.get(HttpHeaders.AccessControlRequestMethod)
          val requestHeaders = request.headers.get(HttpHeaders.AccessControlRequestHeaders)

          val optionsHeaders = PureHeaders();
          optionsHeaders.set(HttpHeaders.AccessControlAllowCredentials, "true")
          optionsHeaders.set(HttpHeaders.AccessControlAllowOrigin, requestOrigin!!)
          optionsHeaders.set(HttpHeaders.AccessControlAllowMethods, requestMethod!!)
          optionsHeaders.set(HttpHeaders.AccessControlAllowHeaders, requestHeaders!!)
          return@definePureResponse PureResponse(HttpStatusCode.OK, optionsHeaders)
        }

        if (url.protocol != URLProtocol.HTTP && url.protocol != URLProtocol.HTTPS) {
          throwException(
            HttpStatusCode.BadRequest,
            "invalid request protocol: ${url.protocol.name}"
          )
        }
        val pureRequest = PureClientRequest(
          href = url.toString(),
          method = request.method,
          headers = request.headers,
          body = request.body,
          from = request.from
        )
        /// 如果是options类型的请求，直接放行，不做任何同域验证
        if (pureRequest.method == PureMethod.OPTIONS) {
          return@definePureResponse httpFetch(pureRequest)
        }
        val isSameOrigin =
          url.host.let { host -> host == ipc.remote.mmid || host.endsWith(".${ipc.remote.mmid}") }
        /// 否则如果域名，那么才能直接放行
        if (isSameOrigin) {
          return@definePureResponse httpFetch(pureRequest)
        }
        /// 如果不是同域，需要进行跨域判定

        // 首先根据标准，判断是否需要进行 options 请求请求options获取 allow-method
        val needPreflightRequest = when (pureRequest.method) {
          PureMethod.GET, PureMethod.POST, PureMethod.HEAD -> {
            var isSimple = true
            for ((key, value) in pureRequest.headers) {
//              if (key in preflightRequestHeaderKeys) {
//                isSimple = false
//                break
//              }
              if (!isSimpleHeader(key, value)) {
                isSimple = false
                break
              }
            }

            // No ReadableStream object is used in the request.
            if (pureRequest.body is PureStreamBody) {
              isSimple = false
            }

            !isSimple
          }

          else -> false
        }

        /**
         * 如果需要发起“预检请求”，那么根据预检请求返回一个专门用于 cors 的 request 对象
         */
        val corsRequest = if (needPreflightRequest) {
          var accessControlRequestHeaders = ""
          val needPreflightHeaders = PureHeaders()
          for ((key, value) in pureRequest.headers) {
            if (!isSimpleHeader(key, value)) {
              accessControlRequestHeaders += "$key,"
            } else {
              // CORS-preflight requests must never include credentials.
              if (key !in credentialsHeaderKeys) {
                needPreflightHeaders.set(key, value)
              }
            }
          }

          accessControlRequestHeaders.isNotBlank().trueAlso {
            accessControlRequestHeaders.replaceAfterLast(",", "")
            needPreflightHeaders.set(
              HttpHeaders.AccessControlRequestHeaders,
              accessControlRequestHeaders
            )
          }
          needPreflightHeaders.set(
            HttpHeaders.AccessControlRequestMethod,
            pureRequest.method.method
          )

          val optionsResponse =
            httpFetch(PureClientRequest(pureRequest.href, PureMethod.OPTIONS, needPreflightHeaders))
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
            else -> allowOrigin.split(",")
              .find { it == pureRequest.url.protocolWithAuthority } == null
          }.falseAlso {
            throwException(HttpStatusCode.NotAcceptable, "no-cors by origin")
          }

          when (allowMethods) {
            null -> when (request.method) {
              PureMethod.GET, PureMethod.POST, PureMethod.HEAD -> true;
              else -> false
            }

            "*" -> true
            else -> request.method.method in allowMethods.split(',')
          }.falseAlso {
            throwException(HttpStatusCode.NotAcceptable, "no-cors by method")
          }

          PureClientRequest(
            url.toString(),
            pureRequest.method,
            PureHeaders(pureRequest.headers.toList().filter {
              if (isSimpleHeader(it.first, it.second))
                true
              else it.first in allowHeaders
            }),
            body = pureRequest.body,
            from = pureRequest.from
          )
        } else {
          pureRequest
        }
        // 正式发起请求
        val corsResponse = httpFetch(corsRequest)
        if (corsRequest.headers.has("Cookie") || corsRequest.headers.has("Authorization") && corsResponse.headers.get(
            HttpHeaders.AccessControlAllowCredentials
          ) != "true"
        ) {
          throwException(HttpStatusCode.NoContent)
        }
        /// AccessControlExposeHeaders 默认不需要工作
        val requestOrigin = request.headers.get(HttpHeaders.Origin)
        corsResponse.headers.set(HttpHeaders.AccessControlAllowOrigin, requestOrigin!!)
        corsResponse.headers.set(HttpHeaders.AccessControlAllowCredentials, "true")

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

  private val credentialsHeaderKeys = setOf(
    "Cookie",
    "Authorization"
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
    token: String, message: PureServerRequest, routes: List<CommonRoute>
  ): PureStream {
    debugHttp("LISTEN", tokenMap.keys.toList())
    val gateway = tokenMap[token] ?: throw Exception("no gateway with token: $token")
    debugHttp("LISTEN/start", "host: ${gateway.urlInfo.host}, token: $token")

    val streamIpc = ReadableStreamIpc(
      gateway.listener.mainIpc.remote, "http-gateway/${gateway.urlInfo.host}"
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

fun findDwebGateway(request: PureServerRequest): DwebGatewayInfo? {
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
