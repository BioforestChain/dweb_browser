package org.dweb_browser.microservice.help

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.server.plugins.origin
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.ApplicationResponse
import io.ktor.server.response.header
import io.ktor.server.response.respondOutputStream
import io.ktor.utils.io.jvm.javaio.toInputStream
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
  out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE
): Long {
  var bytesCopied: Long = 0
  val buffer = ByteArray(bufferSize)
  try {
    var bytes = read(buffer)
    while (bytes >= 0) {
      out.write(buffer, 0, bytes)
      out.flush()
      bytesCopied += bytes
      bytes = read(buffer)
    }
  } catch (e: Exception) {
    close()
    throw e
  }
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
  .body(this.bodyAsChannel().toInputStream(), this.contentLength())

