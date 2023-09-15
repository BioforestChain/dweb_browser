package org.dweb_browser.microservice.help

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.ApplicationResponse
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondNullable
import io.ktor.server.response.respondText
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.bits.copyTo
import io.ktor.utils.io.copyAndClose
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.read
import io.ktor.utils.io.writeAvailable
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.readByteArray
import org.dweb_browser.microservice.http.IPureBody
import org.dweb_browser.microservice.http.PureBinaryBody
import org.dweb_browser.microservice.http.PureEmptyBody
import org.dweb_browser.microservice.http.PureRequest
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.http.PureStreamBody
import org.dweb_browser.microservice.http.PureStringBody
import org.dweb_browser.microservice.ipc.helper.DEFAULT_BUFFER_SIZE
import org.dweb_browser.microservice.ipc.helper.IpcHeaders
import org.dweb_browser.microservice.ipc.helper.IpcMethod
import org.dweb_browser.microservice.ipc.helper.ReadableStream
import org.dweb_browser.microservice.ipc.helper.ReadableStreamOut
import org.dweb_browser.microservice.ipc.helper.debugStream

fun debugHelper(tag: String, msg: Any = "", err: Throwable? = null) =
  printDebug("helper", tag, msg, err)

fun ApplicationRequest.asPureRequest(): PureRequest {
  val ipcMethod = IpcMethod.from(httpMethod)
  val ipcHeaders = IpcHeaders(headers)
  return PureRequest(
    uri, ipcMethod, ipcHeaders, body = if (//
      (ipcMethod == IpcMethod.GET && !isWebSocket(ipcMethod, ipcHeaders)) //
      || ipcHeaders.get("Content-Length") == "0"
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
        contentType = ContentType.parse(value)
        continue
      }

      "Content-Length" -> {
        contentLength = value.toLong()
      }
    }
    header(key, value)
  }
  when (val pureBody = response.body) {
    is PureEmptyBody -> this.call.respondNullable<String?>(response.status, null)
    is PureStringBody -> this.call.respondText(
      text = pureBody.toPureString(), status = response.status
    )

    is PureBinaryBody -> this.call.respondBytes(
      bytes = pureBody.toPureBinary(), status = response.status
    )

    is PureStreamBody -> this.call.respondBytesWriter(
      contentType = contentType,
      status = response.status,
      contentLength = contentLength,
    ) {
      pureBody.toPureStream().getReader("ktorAndHttp4kHelper toApplicationResponse")
        .copyAndClose(this)
    }
  }
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
  httpRequestBuilder.url(this.href)
  for ((key, value) in this.headers.toMap()) {
    httpRequestBuilder.headers.append(key, value ?: "")
  }
  httpRequestBuilder.setBody(this.body.toPureStream().getReader("toHttpRequestBuilder"))
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
    consumeEachArrayRange { byteArray, last ->
      debugStream("toReadableStream", "SS[$id] enqueue ${byteArray.size}")
      controller.enqueue(byteArray)
      if (last) {
        debugStream("toReadableStream", "SS[$id] end")
        controller.close()
      }
    }
  }


/**
 * Visitor function that is invoked for every available buffer (or chunk) of a channel.
 * The last parameter shows that the buffer is known to be the last.
 */
typealias ConsumeEachArrayVisitor = ConsumeEachArrayRangeController. (byteArray: ByteArray, last: Boolean) -> Unit

class ConsumeEachArrayRangeController() {
  var continueFlag = true
  fun breakLoop() {
    continueFlag = false
  }
}

/**
 * For every available bytes range invokes [visitor] function until it return false or end of stream encountered.
 * The provided buffer should be never captured outside of the visitor block otherwise resource leaks, crashes and
 * data corruptions may occur. The visitor block may be invoked multiple times, once or never.
 */
suspend inline fun ByteReadChannel.consumeEachArrayRange(
  visitor: ConsumeEachArrayVisitor,
) {
  val controller = ConsumeEachArrayRangeController()
  do {
    var lastChunkReported = false
    read { source, start, endExclusive ->
      val nioBuffer: ByteArray = when {
        endExclusive > start -> source.slice(start, endExclusive - start).run {
          val remaining = (endExclusive - start).toInt()
          val res = ByteArray(remaining)
          copyTo(res, start, remaining)
          res
        }

        else -> ByteArray(0)
      }

      lastChunkReported = availableForRead == 0 && isClosedForWrite
      controller.visitor(nioBuffer, lastChunkReported)

      nioBuffer.size
    }

    if (lastChunkReported && isClosedForRead) {
      break
    }
  } while (controller.continueFlag)
}

val ByteReadChannel.canRead get() = !(availableForRead == 0 && isClosedForWrite && isClosedForRead)
suspend fun ByteReadChannel.readAvailablePacket() = readPacket(availableForRead)
suspend fun ByteReadChannel.readAvailableByteArray() = readAvailablePacket().readByteArray()
