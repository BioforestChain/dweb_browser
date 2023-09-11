package org.dweb_browser.microservice.help

import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.utils.io.ByteReadChannel
import org.dweb_browser.microservice.http.PureRequest
import org.dweb_browser.microservice.http.PureStreamBody
import org.dweb_browser.microservice.http.PureUtf8StringBody
import org.dweb_browser.microservice.ipc.helper.IpcHeaders
import org.dweb_browser.microservice.ipc.helper.IpcMethod

data class InitRequest(
  val method: IpcMethod,
  val headers: IpcHeaders,
  val body: Any?
)

fun isWebSocket(method: IpcMethod, headers: IpcHeaders): Boolean {
  val upgrade = headers.get("Upgrade") == "websocket"
  return method.method == "GET" && upgrade
}

fun httpMethodCanOwnBody(
  method: HttpMethod,
  headers: Headers? = null
): Boolean {

  if (headers != null) {
    return isWebSocket(IpcMethod.from(method), IpcHeaders(headers))
  }
  return method != HttpMethod.Get &&
      method != HttpMethod.Head &&
      method != HttpMethod.Options
}

/**
 * 构建Request对象,和`new Request`类似,允许突破原本Request的一些限制
 * @param toRequest
 */
fun buildRequestX(url: String, initRequest: InitRequest): PureRequest {
  val method = initRequest.method
  val headers = initRequest.headers
  val body = initRequest.body
  val isWs = isWebSocket(method, headers)

  val request = PureRequest(method = method, url = url, headers = headers).let { req ->
    if (req.method == IpcMethod.GET || req.method == IpcMethod.HEAD) {
    } else if (isWs) {
      req.method = IpcMethod.POST
      req.body = PureStreamBody(body as ByteReadChannel)
    } else if (body is PureStreamBody) {
      req.body = PureStreamBody(body.stream)
    } else when (body) {
      is String -> req.body = PureUtf8StringBody(body)
      is ByteArray -> req.body = PureStreamBody(ByteReadChannel(body))
      is ByteReadChannel -> req.body = PureStreamBody(body)
      else -> throw Exception("invalid body to request: $body")
    }

    req
  }
  // 再强制改为GET
  if (isWs) {
    request.method = IpcMethod.GET
  }
  return request
}