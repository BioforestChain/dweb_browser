package org.dweb_browser.pure.http.ktor

import io.ktor.client.utils.EmptyContent
import io.ktor.http.ContentType
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
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
      val nativeStream = when (val stream =
        pureBody.toPureStream().getReader("toApplicationResponse")) {
        is ByteReadChannelDelegate -> stream.sourceByteReadChannel
        else -> stream
      }
      nativeStream.copyAndClose(this)
    }
  }
}

fun ApplicationRequest.asPureRequest(): PureServerRequest {
  val pureMethod = org.dweb_browser.pure.http.PureMethod.from(httpMethod)
  val pureHeaders = PureHeaders(headers)
  return PureServerRequest(
    uri, pureMethod, pureHeaders,
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