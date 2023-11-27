package org.dweb_browser.core.help

import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.utils.io.ByteReadChannel
import org.dweb_browser.core.http.IPureBody
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureStreamBody
import org.dweb_browser.core.http.PureStringBody
import org.dweb_browser.core.ipc.helper.IpcHeaders
import org.dweb_browser.core.ipc.helper.IpcMethod

data class InitRequest(
  val method: IpcMethod, val headers: IpcHeaders, val body: Any?
)

fun isWebSocket(method: IpcMethod, headers: IpcHeaders): Boolean {
  val upgrade = headers.get("Upgrade") == "websocket"
  return method.method == "GET" && upgrade
}

fun httpMethodCanOwnBody(
  method: HttpMethod, headers: Headers? = null
): Boolean {

  if (headers != null) {
    return isWebSocket(IpcMethod.from(method), IpcHeaders(headers))
  }
  return method != HttpMethod.Get && method != HttpMethod.Head && method != HttpMethod.Options
}

/**
 * 构建Request对象,和`new Request`类似,允许突破原本Request的一些限制
 * @param toRequest
 */
fun buildRequestX(
  url: String, method: IpcMethod, headers: IpcHeaders, body: String?
) = buildRequestX(url, method, headers, anyBody = body)

fun buildRequestX(
  url: String, method: IpcMethod, headers: IpcHeaders, body: ByteArray?
) = buildRequestX(url, method, headers, anyBody = body)

fun buildRequestX(
  url: String, method: IpcMethod, headers: IpcHeaders, body: ByteReadChannel?
) = buildRequestX(url, method, headers, anyBody = body)

fun buildRequestX(
  url: String, method: IpcMethod, headers: IpcHeaders, body: IPureBody?
) = buildRequestX(url, method, headers, anyBody = body)

internal fun buildRequestX(
  url: String, method: IpcMethod, headers: IpcHeaders, anyBody: Any?
): PureRequest {
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

  return PureRequest(method = method, href = url, headers = headers, body = pureBody);
}