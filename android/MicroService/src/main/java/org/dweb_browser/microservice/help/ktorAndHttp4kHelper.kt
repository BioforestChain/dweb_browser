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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.printdebugln
import org.dweb_browser.microservice.ipc.helper.ReadableStream
import org.http4k.core.Headers
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.server.supportedOrNull
import java.io.InputStream
import java.io.OutputStream
import io.ktor.http.Headers as KtorHeaders

fun debugHelper(tag: String, msg: Any = "", err: Throwable? = null) =
  printdebugln("helper", tag, msg, err)


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

private fun InputStream.copyToWithFlush(
  output: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE
): Long {
  println("GG copyToWithFlush start")
  var bytesCopied: Long = 0
  val buffer = ByteArray(bufferSize)
  try {
    do {
      when (val canReadSize = available()) {
        0, -1 -> {
          println("GG copyToWithFlush no byte!($canReadSize)")
          output.flush()
          break
        }

        else -> {
          println("GG copyToWithFlush can bytes($canReadSize)")
          val readSize = read(buffer)
          println("GG copyToWithFlush $readSize/$canReadSize bytes")
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
  println("GG copyToWithFlush end")
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

suspend fun HttpResponse.toResponse() = Response(
  Status(this.status.value, this.status.description),
  this.version.toString()
)
  .headers(this.headers.toHttp4kHeaders())
  .let {
    val channel = this.bodyAsChannel()
    val bodyLen = this.contentLength()
    println("GG toResponse")
    it.body(channel.toReadableStream(), bodyLen)
  }

suspend fun ByteReadChannel.toReadableStream() = ReadableStream(onStart = { controller ->
  CoroutineScope(ioAsyncExceptionHandler).launch {
    this@toReadableStream.consumeEachBufferRange { byteArray, last ->
      controller.enqueue(byteArray.moveToByteArray())
      if (last) {
        controller.close()
      }
      true
    }
  }
})