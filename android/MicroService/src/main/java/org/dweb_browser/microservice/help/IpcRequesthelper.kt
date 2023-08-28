package org.dweb_browser.microservice.help

import org.http4k.core.Body
import org.http4k.core.Headers
import org.http4k.core.Method
import org.http4k.core.Request
import java.io.InputStream

data class InitRequest(
  val method: Method,
  val headers: Headers,
  val body: Any?
)

fun isWebSocket(method: Method, headers: Headers): Boolean {
  val upgrade = headers.contains(Pair("Upgrade", "websocket"))
  return method.name == "GET" && upgrade
}

fun httpMethodCanOwnBody(
  method: Method,
  headers: Headers? = null
): Boolean {

  if (headers != null) {
    return isWebSocket(method, headers)
  }
  return method != Method.GET &&
      method != Method.HEAD &&
      method != Method.TRACE &&
      method != Method.OPTIONS
}

/**
 * 构建Request对象,和`new Request`类似,允许突破原本Request的一些限制
 * @param toRequest
 */
fun buildRequestX(url: String, initRequest: InitRequest): Request {

  val method = initRequest.method
  val headers = initRequest.headers
  val body = initRequest.body
  val isWs = isWebSocket(method, headers.toList())

  val request = Request(method, url).headers(headers.toList()).let { req ->
    if (req.method == Method.GET || req.method == Method.HEAD) {
      req
    } else if (isWs) {
      req.method(Method.POST)
      req.body(body as InputStream)
    } else if (body is Body) {
      req.body(body.stream)
    } else when (body) {
      is String -> req.body(body)
      is ByteArray -> req.body(body.inputStream(), body.size.toLong())
      is InputStream -> req.body(body)
      else -> throw Exception("invalid body to request: $body")
    }
  }
  // 再强制改为GET
  if (isWs) {
    request.method(Method.GET)
  }
  return request
}