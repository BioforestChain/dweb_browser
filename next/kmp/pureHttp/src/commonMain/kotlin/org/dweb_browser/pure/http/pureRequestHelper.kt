package org.dweb_browser.pure.http

import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.toLowerCasePreservingASCIIRules
import io.ktor.utils.io.ByteReadChannel

data class InitRequest(
  val method: PureMethod, val headers: PureHeaders, val body: Any?
)

const val DEFAULT_BUFFER_SIZE: Int = 8 * 1024

fun isWebSocket(method: PureMethod, headers: PureHeaders): Boolean {
  return method == PureMethod.GET && headers.get("Upgrade") == "websocket"
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

fun dataUriToPureResponse(request: PureRequest): PureResponse {
  val dataUriContent = request.url.fullPath
  val dataUriContentInfo = dataUriContent.split(',', limit = 2)
  when (dataUriContentInfo.size) {
    2 -> {
      val meta = dataUriContentInfo[0]
      val bodyContent = dataUriContentInfo[1]
      val metaInfo = meta.split(';', limit = 2)
//              val response = PureResponse(HttpStatusCode.OK)
      when (metaInfo.size) {
        1 -> {
          return PureResponse(
            HttpStatusCode.OK,
            headers = PureHeaders().apply { set("Content-Type", meta) },
            body = PureStringBody(bodyContent)
          )
        }

        2 -> {
          val encoding = metaInfo[1]
          return if (encoding.trim().toLowerCasePreservingASCIIRules() == "base64") {
            PureResponse(
              HttpStatusCode.OK,
              headers = PureHeaders().apply { set("Content-Type", metaInfo[0]) },
              body = PureBinaryBody(bodyContent.decodeBase64Bytes())
            )
          } else {
            PureResponse(
              HttpStatusCode.OK,
              headers = PureHeaders().apply { set("Content-Type", meta) },
              body = PureStringBody(bodyContent)
            )
          }
        }
      }
    }
  }
  /// 保底操作
  return PureResponse(HttpStatusCode.OK, body = PureStringBody(dataUriContent))
}