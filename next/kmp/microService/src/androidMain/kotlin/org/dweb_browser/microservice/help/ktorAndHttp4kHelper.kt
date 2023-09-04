package org.dweb_browser.microservice.help

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.server.plugins.origin
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.ApplicationResponse
import io.ktor.server.response.header
import io.ktor.server.response.respondOutputStream
import io.ktor.util.moveToByteArray
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.consumeEachBufferRange
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.printDebug
import org.dweb_browser.microservice.ipc.helper.ReadableStream
import org.dweb_browser.microservice.ipc.helper.ReadableStreamOut
import org.dweb_browser.microservice.ipc.helper.debugStream
import org.http4k.core.Headers
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.length
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.server.supportedOrNull
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger
import io.ktor.http.Headers as KtorHeaders

fun debugHelper(tag: String, msg: Any = "", err: Throwable? = null) =
  printDebug("helper", tag, msg, err)


fun ApplicationRequest.asHttp4k() = Method.supportedOrNull(httpMethod.value)?.let {
  Request(it, this@asHttp4k.uri).headers(headers.toHttp4kHeaders())
    .body(receiveChannel().toInputStream(), header("Content-Length")?.toLong()).source(
      RequestSource(
        origin.remoteHost, scheme = origin.scheme
      )
    ) // origin.remotePort does not exist for Ktor
}

private fun KtorHeaders.toHttp4kHeaders(): Headers = names().flatMap { name ->
  (getAll(name) ?: emptyList()).map { name to it }
}

suspend fun ApplicationResponse.fromHttp4K(response: Response) {
  status(HttpStatusCode.fromValue(response.status.code))
  response.headers.filterNot { HttpHeaders.isUnsafe(it.first) || it.first == CONTENT_TYPE.meta.name }
    .forEach { header(it.first, it.second ?: "") }
  call.respondOutputStream(CONTENT_TYPE(response)?.let { ContentType.parse(it.toHeaderValue()) }) {
    response.body.stream.copyToWithFlush(this)
  }
}

var debugStreamAccId = AtomicInteger(1)

private fun InputStream.copyToWithFlush(
  output: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE
): Long {
  val id = debugStreamAccId.incrementAndGet();
  debugStream("copyToWithFlush", "SS[$id] start")
  var bytesCopied: Long = 0
  val buffer = ByteArray(bufferSize)
  try {
    do {
      when (val canReadSize = available()) {
        0, -1 -> {
          debugStream("copyToWithFlush", "SS[$id] no byte!($canReadSize)")
          output.flush()
          break
        }

        else -> {
//          debugStream("copyToWithFlush", "SS[$id] can bytes($canReadSize)")
          val readSize = read(buffer)
//          debugStream("copyToWithFlush", "SS[$id] $readSize/$canReadSize bytes")
          if (readSize > 0) {
            bytesCopied += readSize
            output.write(buffer, 0, readSize)
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

fun Request.isWebSocket() =
  method == Method.GET && (header(HttpHeaders.Connection) == "Upgrade" || header(HttpHeaders.Upgrade) == "websocket")


fun Request.toHttpRequestBuilder() = HttpRequestBuilder().also { httpRequestBuilder ->
  httpRequestBuilder.method = HttpMethod.parse(this.method.name)
  httpRequestBuilder.url(this.uri.toString())
  for ((key, value) in this.headers) {
    httpRequestBuilder.headers.append(key, value ?: "")
  }
  httpRequestBuilder.setBody(this.body.stream)
}

/**
 * 这个函数发生在 prepareRequest(request.toHttpRequestBuilder()).execute 之内，
 * 要求外部传入一个 ReadableStreamOut ，并且一定要在返回 response 后等待 stream.waitClosed
 *
 * 请仔细阅读文档： https://ktor.io/docs/response.html#streaming 了解原因
 */
suspend fun HttpResponse.toResponse(
  streamOut: ReadableStreamOut
) = Response(
  Status(this.status.value, this.status.description),
  this.version.toString(),
)
  .headers(this.headers.toHttp4kHeaders())
  .let {
    /// 如果使用 channel.toReadableStream() 它独立开了一个 CoroutineScope 来做，这与 execute 在 finally 中执行 response.cleanup 是冲突的
    /// 如果使用 channel.toInputStream() 它是阻塞读取，与我们要的效果不符合
    /// 因此我们要外部传入 ReadableStreamOut，我这里返回 Response，同时外部用 readableStream.waitClosed() 来阻止 response.cleanup 的执行

    this.bodyAsChannel().pipeToReadableStream(streamOut.controller)
    val bodyLen = this.contentLength()
    it.body(streamOut.stream, bodyLen)
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

