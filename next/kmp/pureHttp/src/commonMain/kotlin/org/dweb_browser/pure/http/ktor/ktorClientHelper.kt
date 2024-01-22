package org.dweb_browser.pure.http.ktor

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.writeAvailable
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.readByteArray
import org.dweb_browser.pure.http.DEFAULT_BUFFER_SIZE
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureRequest
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureStreamBody
import org.dweb_browser.pure.http.isWebSocket

val debugKtor = Debugger("ktor")

var debugStreamAccId by SafeInt(1)

private suspend fun ByteReadPacket.copyToWithFlush(
  output: ByteWriteChannel, bufferSize: Int = DEFAULT_BUFFER_SIZE
): Long {
  val id = debugStreamAccId++
  debugKtor("copyToWithFlush", "SS[$id] start")
  var bytesCopied: Long = 0
//  val buffer = ByteArray(bufferSize)
  try {
    do {
      when (val canReadSize = remaining) {
        0L, -1L -> {
          debugKtor("copyToWithFlush", "SS[$id] no byte!($canReadSize)")
          output.flush()
          break
        }

        else -> {
          debugKtor("copyToWithFlush", "SS[$id] can bytes($canReadSize)")
          val buffer = readByteArray()

          debugKtor("copyToWithFlush", "SS[$id] ${buffer.size}/$canReadSize bytes")
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
    debugKtor("InputStream.copyToWithFlush", "", e)
  }
  debugKtor("copyToWithFlush", "SS[$id] end")
  return bytesCopied
}

fun PureRequest.isWebSocket() = isWebSocket(this.method, this.headers)

fun PureClientRequest.toHttpRequestBuilder() = HttpRequestBuilder().also { httpRequestBuilder ->
  httpRequestBuilder.fromPureClientRequest(this)
}

fun HttpRequestBuilder.fromPureClientRequest(pureRequest: PureClientRequest) {
  this.method = HttpMethod.parse(pureRequest.method.name)
  // 确保默认端口正确
  this.url.port = pureRequest.url.protocol.defaultPort
  this.url(pureRequest.href)

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
