package org.dweb_browser.pure.http.ktor

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.pure.http.PureStreamBody

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

fun OutgoingContent.toPureBody(): IPureBody =
  when (this) {
    is OutgoingContent.ByteArrayContent -> IPureBody.from(bytes())
    is OutgoingContent.NoContent -> IPureBody.Empty
    is OutgoingContent.ProtocolUpgrade -> throw Exception("no support ProtocolUpgrade")
    is OutgoingContent.ReadChannelContent -> IPureBody.from(
      PureStream(readFrom())
    )

    is OutgoingContent.WriteChannelContent -> throw Exception("no support WriteChannelContent")
    is OutgoingContent.ContentWrapper -> delegate().toPureBody()
  }
