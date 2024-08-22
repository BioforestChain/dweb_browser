package org.dweb_browser.core.http

import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.help.AdapterManager
import org.dweb_browser.core.http.router.ResponseException
import org.dweb_browser.core.std.http.debugHttp
import org.dweb_browser.pure.http.HttpPureServer
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureServerRequest

fun interface HttpGateway {
  suspend fun onRequest(request: PureServerRequest): PureResponse?
}

object dwebHttpGatewayService {
  val gatewayAdapterManager = DwebGatewayHandlerAdapterManager()

  val server = HttpPureServer { rawRequest ->
    val pureRequest = when (val url = rawRequest.queryOrNull("X-Dweb-Url")) {
      null -> rawRequest
      else -> rawRequest.copy(href = url)
    }
    debugHttp("doGateway", pureRequest.href)
    gatewayAdapterManager.doGateway(pureRequest)
  }

  suspend fun getPort(): UShort {
    return server.stateFlow.value ?: server.start(0u)
  }

  suspend fun getHttpLocalhostGatewaySuffix() = ".localhost:${getPort()}"

  suspend fun getUrl() = "http://127.0.0.1:${getPort()}"
}

class DwebGatewayHandlerAdapterManager : AdapterManager<HttpGateway>() {
  suspend fun doGateway(request: PureServerRequest): PureResponse? {
    try {
      for (adapter in adapters) {
        val response = adapter.onRequest(request)
        if (response != null) {
          return response
        }
      }
      return null
    } catch (e: ResponseException) {
      return PureResponse(
        HttpStatusCode(e.code.value, e.message),
        body = IPureBody.from(e.cause?.message ?: "")
      )
    } catch (e: Throwable) {
      return PureResponse(
        HttpStatusCode.InternalServerError,
        body = IPureBody.from(e.message ?: "")
      )
    }
  }
}
