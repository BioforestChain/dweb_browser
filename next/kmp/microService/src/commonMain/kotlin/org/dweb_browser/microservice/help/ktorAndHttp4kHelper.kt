package org.dweb_browser.microservice.help

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.printDebug
import org.dweb_browser.microservice.ipc.helper.ReadableStream
import org.dweb_browser.microservice.ipc.helper.ReadableStreamOut
import org.dweb_browser.microservice.ipc.helper.debugStream
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.writeAvailable
import kotlinx.atomicfu.atomic
import org.dweb_browser.helper.readByteArray
import org.dweb_browser.microservice.http.PureRequest
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.http.PureStreamBody
import org.dweb_browser.microservice.ipc.helper.DEFAULT_BUFFER_SIZE
import org.dweb_browser.microservice.ipc.helper.IpcHeaders
import org.dweb_browser.microservice.ipc.helper.IpcMethod

fun debugHelper(tag: String, msg: Any = "", err: Throwable? = null) =
  printDebug("helper", tag, msg, err)

fun ApplicationRequest.asPureRequest(): PureRequest {
  return PureRequest(
    this.uri,
    IpcMethod.from(this.httpMethod),
    IpcHeaders(this.headers),
    PureStreamBody(receiveChannel())
  )
}

var debugStreamAccId = atomic(1)

private suspend fun ByteReadPacket.copyToWithFlush(
  output: ByteWriteChannel, bufferSize: Int = DEFAULT_BUFFER_SIZE
): Long {
  val id = debugStreamAccId.incrementAndGet();
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
  } catch (e: Exception) {
    // 有异常，那么可能是 output 的写入出现的异常，这时候需要将 input 也给关闭掉，因为已经不再读取了
    close()
    debugHelper("InputStream.copyToWithFlush", "", e)
  }
  debugStream("copyToWithFlush", "SS[$id] end")
  return bytesCopied
}

fun PureRequest.isWebSocket() = isWebSocket(this.method, this.headers)

fun PureRequest.toHttpRequestBuilder() = HttpRequestBuilder().also { httpRequestBuilder ->
  httpRequestBuilder.method = HttpMethod.parse(this.method.name)
  httpRequestBuilder.url(this.url)
  for ((key, value) in this.headers.toMap()) {
    httpRequestBuilder.headers.append(key, value ?: "")
  }
  httpRequestBuilder.setBody(this.body.toStream())
}

/**
 * 这个函数发生在 prepareRequest(request.toHttpRequestBuilder()).execute 之内，
 * 要求外部传入一个 ReadableStreamOut ，并且一定要在返回 response 后等待 stream.waitClosed
 *
 * 请仔细阅读文档： https://ktor.io/docs/response.html#streaming 了解原因
 */
suspend fun HttpResponse.toResponse(
  streamOut: ReadableStreamOut
): PureResponse {
  this.bodyAsChannel().pipeToReadableStream(streamOut.controller)
  return PureResponse(
    HttpStatusCode(this.status.value, this.status.description),
    IpcHeaders(this.headers),
    body = PureStreamBody(streamOut.stream.stream)
  )
}

fun ByteReadChannel.toReadableStream() = ReadableStream(onStart = { pipeToReadableStream(it) })

fun ByteReadChannel.pipeToReadableStream(controller: ReadableStream.ReadableStreamController) =
  CoroutineScope(ioAsyncExceptionHandler).launch {
    val id = debugStreamAccId.incrementAndGet();
    debugStream("toReadableStream", "SS[$id] start")
    consumeEachBufferRange { byteArray, last ->
      debugStream("toReadableStream", "SS[$id] enqueue ${byteArray.length()}")
      controller.enqueue(byteArray.moveToByteArray())
      if (last) {
        debugStream("toReadableStream", "SS[$id] end")
        controller.close()
      }
      true
    }
  }

