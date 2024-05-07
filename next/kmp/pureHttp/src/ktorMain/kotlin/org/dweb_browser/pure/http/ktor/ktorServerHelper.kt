package org.dweb_browser.pure.http.ktor

import io.ktor.client.utils.EmptyContent
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.server.plugins.origin
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.httpMethod
import io.ktor.server.response.ApplicationResponse
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondText
import io.ktor.utils.io.copyAndClose
import org.dweb_browser.helper.ByteReadChannelDelegate
import org.dweb_browser.pure.http.PureBinaryBody
import org.dweb_browser.pure.http.PureEmptyBody
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureServerRequest
import org.dweb_browser.pure.http.PureStreamBody
import org.dweb_browser.pure.http.PureStringBody
import org.dweb_browser.pure.http.debugKtor
import org.dweb_browser.pure.http.isWebSocket

suspend fun ApplicationResponse.fromPureResponse(response: PureResponse) {
  status(response.status)

  var contentType: ContentType? = null
  var contentLength: Long? = null
  for ((key, value) in response.headers.toHttpHeaders()) {
    when (key) {
      "Content-Type" -> {
        contentType = try {
          ContentType.parse(value)
        } catch (e: Throwable) {
          debugKtor("fromPureResponse", "ContentType.parse($value)", e)
          null
        }
        continue
      }

      "Content-Length" -> {
        contentLength = value.toLong()
        continue
      }
    }
    header(key, value)
  }
  when (val pureBody = response.body) {
    is PureEmptyBody -> this.call.respond(
      status = response.status, message = EmptyContent
    )

    is PureStringBody -> this.call.respondText(
      contentType = contentType,
      text = pureBody.toPureString(),
      status = response.status
    )

    is PureBinaryBody -> this.call.respondBytes(
      contentType = contentType,
      bytes = pureBody.toPureBinary(),
      status = response.status
    )

    is PureStreamBody -> this.call.respondBytesWriter(
      contentType = contentType,
      status = response.status,
      contentLength = contentLength,
    ) {
      val stream =
        pureBody.toPureStream().getReader("toApplicationResponse")

      runCatching {
        when (stream) {
          is ByteReadChannelDelegate -> stream.sourceByteReadChannel
          else -> stream
        }.copyAndClose(this)
      }.getOrElse {
        // 接收端关闭了写，所以我这边也要关闭自己这个流
        stream.cancel(it)
      }
    }
  }
}

fun ApplicationRequest.asPureRequest(): PureServerRequest {
  val pureMethod = org.dweb_browser.pure.http.PureMethod.from(httpMethod)
  val pureHeaders = PureHeaders(headers)
  // 对http2的支持
  if (!pureHeaders.has("Host")) {
    pureHeaders.set("Host", local.serverHost)
  }
  return PureServerRequest(
    // 组装出完整的url
    origin.run {
      "$scheme://$serverHost${if (URLProtocol.byName[scheme]?.defaultPort == serverPort) "" else ":$serverPort"}$uri"
    },
    pureMethod, pureHeaders,
    body = if (//
      (pureMethod == org.dweb_browser.pure.http.PureMethod.GET && !isWebSocket(
        pureMethod,
        pureHeaders
      )) //
      || pureHeaders.get("Content-Length") == "0"
    ) org.dweb_browser.pure.http.IPureBody.Empty
    else PureStreamBody(receiveChannel()),
    from = this,
  )
}