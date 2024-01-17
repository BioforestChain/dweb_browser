package org.dweb_browser.pure.http

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.utils.EmptyContent
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.ApplicationResponse
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondText
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.copyAndClose
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.writeAvailable
import org.dweb_browser.helper.ByteReadChannelDelegate
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.readByteArray

val debugHelper = Debugger("helper")
val debugKtor = Debugger("ktor")
val debugStream = Debugger("stream")

fun ApplicationRequest.asPureRequest(): PureServerRequest {
  val pureMethod = PureMethod.from(httpMethod)
  val pureHeaders = PureHeaders(headers)
  return PureServerRequest(
    uri, pureMethod, pureHeaders,
    body = if (//
      (pureMethod == PureMethod.GET && !isWebSocket(pureMethod, pureHeaders)) //
      || pureHeaders.get("Content-Length") == "0"
    ) IPureBody.Empty
    else PureStreamBody(receiveChannel()),
    from = this,
  )
}

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

var debugStreamAccId by SafeInt(1)

private suspend fun ByteReadPacket.copyToWithFlush(
  output: ByteWriteChannel, bufferSize: Int = DEFAULT_BUFFER_SIZE
): Long {
  val id = debugStreamAccId++
  debugStream("copyToWithFlush", "SS[$id] start")
  var bytesCopied: Long = 0
//  val buffer = ByteArray(bufferSize)
  try {
    do {
      when (val canReadSize = remaining) {
        0L, -1L -> {
          debugStream("copyToWithFlush", "SS[$id] no byte!($canReadSize)")
          output.flush()
          break
        }

        else -> {
          debugStream("copyToWithFlush", "SS[$id] can bytes($canReadSize)")
          val buffer = readByteArray()

          debugStream("copyToWithFlush", "SS[$id] ${buffer.size}/$canReadSize bytes")
          if (buffer.isNotEmpty()) {
            bytesCopied += buffer.size
            output.writeAvailable(src = buffer)
            output.flush()
          } else {
            break
          }
        }
      }
    } while (true)
  } catch (e: Throwable) {
    // 有异常，那么可能是 output 的写入出现的异常，这时候需要将 input 也给关闭掉，因为已经不再读取了
    close()
    debugHelper("InputStream.copyToWithFlush", "", e)
  }
  debugStream("copyToWithFlush", "SS[$id] end")
  return bytesCopied
}

fun PureRequest.isWebSocket() = isWebSocket(this.method, this.headers)

fun PureClientRequest.toHttpRequestBuilder() = HttpRequestBuilder().also { httpRequestBuilder ->
  httpRequestBuilder.fromPureClientRequest(this)
}

fun HttpRequestBuilder.fromPureClientRequest(pureRequest: PureClientRequest) {
  this.method = HttpMethod.parse(pureRequest.method.name)
  this.url(pureRequest.href)
//  this.url.protocol = pureRequest.url.protocol
//  this.url.port = url.protocol.defaultPort

  for ((key, value) in pureRequest.headers.toMap()) {
    this.headers.append(key, value)
  }
  // get请求不能传递body，否则iOS会报错：GET method must not have a body
  if (pureRequest.method != PureMethod.GET) {
    this.setBody(pureRequest.body.toPureStream().getReader("toHttpRequestBuilder"))
  }
}

/**
 * 这个函数发生在 prepareRequest(request.toHttpRequestBuilder()).execute 之内，
 * 所以如果有需要，外部需要这样去构建：
 * .execute {
 *  val bodyReadableStream = it.bodyAsChannel().consumeToReadableStream()
 *  val response = it.toPureResponse(body = PureStreamBody(bodyReadableStream.stream))
 *  bodyReadableStream.waitCanceled()
 * }
 * 要求外部传入一个 ReadableStreamOut ，并且一定要在返回 response 后等待 stream.waitClosed
 *
 * 请仔细阅读文档： https://ktor.io/docs/response.html#streaming 了解原因
 */
suspend fun HttpResponse.toPureResponse(
  status: HttpStatusCode = this.status,
  headers: PureHeaders = PureHeaders(this.headers),
  body: IPureBody? = null,
): PureResponse {
  return PureResponse(
    status = status,
    headers = headers,
    body = body ?: PureStreamBody(this.bodyAsChannel())
  )
}
