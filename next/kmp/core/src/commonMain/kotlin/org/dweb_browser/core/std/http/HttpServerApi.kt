package org.dweb_browser.core.std.http

import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import org.dweb_browser.core.http.router.ResponseException
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.std.http.net.Http1Server
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.base64UrlString
import org.dweb_browser.helper.encodeURI
import kotlin.random.Random

/// TODO installHttpServerApi
suspend fun HttpNMM.HttpRuntime.installHttpServerApi() {
  val serverApi = HttpServerApi(this)
  routes(

  )
}

private class HttpServerApi(private val httpRuntime: HttpNMM.HttpRuntime) {
  /// 注册的域名与对应的 token
  private val tokenMap = SafeHashMap</* token */ String, Gateway>();
  private val gatewayMap = SafeHashMap</* host */ String, Gateway>();

  private val dwebServer = Http1Server()
  private fun getServerUrlInfo(ipc: Ipc, options: DwebHttpServerOptions): ServerUrlInfo {
    val mmid = ipc.remote.mmid
    val subdomainPrefix =
      if (options.subdomain == "" || options.subdomain.endsWith(".")) options.subdomain else "${options.subdomain}."
    val host = "$subdomainPrefix$mmid"
    val internal_origin = "https://$host"
    val public_origin = dwebServer.origin
    return ServerUrlInfo(host, internal_origin, public_origin)
  }

  /**
   * 监听端口，启动服务
   */
  private fun start(ipc: Ipc, options: DwebHttpServerOptions): ServerStartResult {
    val serverUrlInfo = getServerUrlInfo(ipc, options)
    debugHttp("START/start", "$serverUrlInfo => $options")
    if (gatewayMap.contains(serverUrlInfo.host)) {
      throw ResponseException(
        code = HttpStatusCode.BadGateway,
        message = "already in listen: ${serverUrlInfo.internal_origin}"
      )
    }
    val listener = Gateway.PortListener(ipc, serverUrlInfo.host)
    /// ipc 在关闭的时候，自动释放所有的绑定
    ipc.onClosed {
      httpRuntime.scopeLaunch(cancelable = false) {
        debugHttp("start/onClosed") {
          "ipc=${ipc.remote.mmid} host=${serverUrlInfo.host}"
        }
        listener.destroy(CancellationException("ipc closed"))
        close(ipc, options)
      }
    }
    val token = ByteArray(8).also { Random.nextBytes(it) }.base64UrlString

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
    serverIpc: Ipc,
    token: String,
    routes: List<CommonRoute>,
  ) {
    debugHttp("LISTEN/start", token)
    val gateway = tokenMap[token] ?: throw ResponseException(
      code = HttpStatusCode.BadGateway, message = "no gateway with token: $token"
    )

    /// 接收一个body，body在关闭的时候，fetchIpc也会一同关闭
    /// 自己nmm销毁的时候，ipc也会被全部销毁
    /// 自己创建的，就要自己销毁：这个listener被销毁的时候，serverIpc也要进行销毁
    gateway.listener.onDestroy {
      serverIpc.tryClose()
    }

    for (routeConfig in routes) {
      gateway.listener.addRouter(routeConfig, serverIpc)
    }
    debugHttp("LISTEN/end", gateway)
  }

  private fun close(ipc: Ipc, options: DwebHttpServerOptions): Boolean {
    val serverUrlInfo = getServerUrlInfo(ipc, options)
    return gatewayMap.remove(serverUrlInfo.host)?.let { gateway ->
      debugHttp("close", "mmid: ${ipc.remote.mmid} ${serverUrlInfo.host}")
      tokenMap.remove(gateway.token)
      gateway.listener.destroyDeferred.complete(null)
      true
    } ?: false
  }
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

@Serializable
data class ServerStartResult(val token: String, val urlInfo: ServerUrlInfo)