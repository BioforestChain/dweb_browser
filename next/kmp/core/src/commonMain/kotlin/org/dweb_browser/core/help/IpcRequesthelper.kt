package org.dweb_browser.core.help

import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.utils.io.ByteReadChannel
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureStreamBody
import org.dweb_browser.pure.http.PureStringBody
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod

data class InitRequest(
  val method: PureMethod, val headers: PureHeaders, val body: Any?
)

fun isWebSocket(method: PureMethod, headers: PureHeaders): Boolean {
  val upgrade = headers.get("Upgrade") == "websocket"
  return method.method == "GET" && upgrade
}

fun httpMethodCanOwnBody(
  method: HttpMethod, headers: Headers? = null
): Boolean {

  if (headers != null) {
    return isWebSocket(PureMethod.from(method), PureHeaders(headers))
  }
  return method != HttpMethod.Get && method != HttpMethod.Head && method != HttpMethod.Options
}

/**
 * 构建Request对象,和`new Request`类似,允许突破原本Request的一些限制
 * @param toRequest
 */
fun buildRequestX(
  url: String, method: PureMethod, headers: PureHeaders, body: String?
) = buildRequestX(url, method, headers, anyBody = body)

fun buildRequestX(
  url: String, method: PureMethod, headers: PureHeaders, body: ByteArray?
) = buildRequestX(url, method, headers, anyBody = body)

fun buildRequestX(
  url: String, method: PureMethod, headers: PureHeaders, body: ByteReadChannel?
) = buildRequestX(url, method, headers, anyBody = body)

fun buildRequestX(
  url: String, method: PureMethod, headers: PureHeaders, body: IPureBody?, from: Any? = null
) = buildRequestX(url, method, headers, anyBody = body, from = from)

internal fun buildRequestX(
  url: String, method: PureMethod, headers: PureHeaders, anyBody: Any?, from: Any? = null
): PureClientRequest {
  val pureBody: IPureBody = when (anyBody) {
    is String -> if (anyBody.isEmpty()) IPureBody.Empty else PureStringBody(anyBody)
    is ByteArray -> if (anyBody.isEmpty()) IPureBody.Empty else PureStreamBody(
      ByteReadChannel(
        anyBody
      )
    )

    is ByteReadChannel -> PureStreamBody(anyBody)
    is IPureBody -> anyBody
    null -> IPureBody.Empty
    else -> throw Exception("invalid body to request: $anyBody")
  }

  return PureClientRequest(
    method = method,
    href = url,
    headers = headers,
    body = pureBody,
    from = from
  );
}