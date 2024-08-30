package org.dweb_browser.core.std.http

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import org.dweb_browser.core.http.dwebHttpGatewayService
import org.dweb_browser.core.http.dwebProxyService
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.pure.http.HttpPureClient
import org.dweb_browser.pure.http.HttpPureClientConfig
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse

val DWEB_PING_URI = "/--dweb-ping-${randomUUID()}--"

suspend fun HttpNMM.HttpRuntime.installHttpServerPingPong() {
  /// 添加基础响应服务
  dwebHttpGatewayService.gatewayAdapterManager.append(10000) { request ->
    val encodedPath = request.url.encodedPath
    println("QAQ dwebPingPong $encodedPath")
    if (encodedPath == DWEB_PING_URI || (debugHttp.isEnable && encodedPath == "/--dweb-ping--")) {
      PureResponse.build {
        appendHeaders(CORS_HEADERS)
        body("""
          --dweb-pong--
          uri=${encodedPath}
          headers=${
          request.headers.toMap().entries.joinToString(", ") {
            "'${it.key}':'${it.value}'"
          }
        }
          server=${dwebHttpGatewayService.server.getDebugInfo()}
          """.trimIndent())
      }
    } else null
  }
  var httpClient =
    HttpPureClient(HttpPureClientConfig(httpProxyUrl = dwebProxyService.proxyUrl.value))
  dwebProxyService.proxyUrl.collectIn(this.getRuntimeScope()) {
    httpClient = HttpPureClient(HttpPureClientConfig(httpProxyUrl = it))
  }

  /**
   * 按需轮训基础响应服务
   * 之所以会有这个功能，是因为程序可能会被冻结内存（IOS平台），唤醒后，端口失去绑定，我们需要有一个机制来发现这个问题
   */
  scopeLaunch(cancelable = true) {
    val internalTime = 1000L
    var preTime = datetimeNow()
    while (true) {
      delay(internalTime)
      val nowTime = datetimeNow()
      val diffTime = nowTime - preTime
      if (diffTime > internalTime + 100) {
        debugHttp("ping-pong-loop") { "internal timeout(${(diffTime / 1000f)}s)!" }
        @OptIn(ExperimentalCoroutinesApi::class) val response = select {
          async {
            httpClient.fetch(
              PureClientRequest("https://internal.dweb/$DWEB_PING_URI", PureMethod.GET)
            )
          }.onAwait { it }
          onTimeout(100) { PureResponse(HttpStatusCode.RequestTimeout) }
        }
        /// 服务异常，尝试重启
        if (response.status.value != 200) {
          debugHttp("ping-pong-fail") { response.status }
          /// 重启网关服务
          dwebHttpGatewayService.server.close()
          dwebHttpGatewayService.server.start(0u)
          /// 重启代理服务
          dwebProxyService.stop()
          dwebProxyService.start()
        }
        preTime = datetimeNow()
      } else {
        preTime = nowTime
      }
    }
  }
}