package org.dweb_browser.pure.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.ws
import io.ktor.client.request.prepareRequest
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.toLowerCasePreservingASCIIRules
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ioAsyncExceptionHandler

val debugHttpPureClient = Debugger("httpPureClient")

open class KtorPureClient(engine: HttpClientEngineFactory<*>) {
  private val ktorClient = HttpClient(engine) {
    install(HttpTimeout) {
      connectTimeoutMillis = 30_000L
    }
    install(ContentEncoding)
    install(WebSockets)
  }

  suspend fun fetch(request: PureClientRequest): PureResponse {
    try {
      debugHttpPureClient("httpFetch request", request.href)
      if (request.url.protocol.name == "data") {
        val dataUriContent = request.url.fullPath
        val dataUriContentInfo = dataUriContent.split(',', limit = 2)
        when (dataUriContentInfo.size) {
          2 -> {
            val meta = dataUriContentInfo[0]
            val bodyContent = dataUriContentInfo[1]
            val metaInfo = meta.split(';', limit = 2)
//              val response = PureResponse(HttpStatusCode.OK)
            when (metaInfo.size) {
              1 -> {
                return PureResponse(
                  HttpStatusCode.OK,
                  headers = PureHeaders().apply { set("Content-Type", meta) },
                  body = PureStringBody(bodyContent)
                )
              }

              2 -> {
                val encoding = metaInfo[1]
                return if (encoding.trim().toLowerCasePreservingASCIIRules() == "base64") {
                  PureResponse(
                    HttpStatusCode.OK,
                    headers = PureHeaders().apply { set("Content-Type", metaInfo[0]) },
                    body = PureBinaryBody(bodyContent.decodeBase64Bytes())
                  )
                } else {
                  PureResponse(
                    HttpStatusCode.OK,
                    headers = PureHeaders().apply { set("Content-Type", meta) },
                    body = PureStringBody(bodyContent)
                  )
                }
              }
            }
          }
        }
        /// 保底操作
        return PureResponse(HttpStatusCode.OK, body = PureStringBody(dataUriContent))
      }
      val responsePo = CompletableDeferred<PureResponse>()
      CoroutineScope(ioAsyncExceptionHandler).launch {
        try {
          ktorClient.prepareRequest(request.toHttpRequestBuilder()).execute {
            debugHttpPureClient("httpFetch execute", request.href)
            val byteChannel = it.bodyAsChannel()
            val pureStream = PureStream(byteChannel)
            val onClosePo = CompletableDeferred<Unit>()
            pureStream.onClose {
              onClosePo.complete(Unit)
            }
            val response = it.toPureResponse(body = PureStreamBody(pureStream))
            debugHttpPureClient("httpFetch response", request.href)
            responsePo.complete(response)
            onClosePo.await()
          }
          // 线程里面的错误需要在线程里捕捉
        } catch (e: Throwable) {
          // TODO 连接超时提示用户
          debugHttpPureClient("httpFetch error", e.stackTraceToString())
          val response = PureResponse(
            HttpStatusCode.ServiceUnavailable,
            body = PureStringBody(request.url.toString() + "\n" + e.stackTraceToString())
          )
          responsePo.complete(response)
        }
      }
      return responsePo.await()
    } catch (e: Throwable) {
      debugHttpPureClient("httpFetch", request.url, e)
      return PureResponse(
        HttpStatusCode.ServiceUnavailable,
        body = PureStringBody(request.url.toString() + "\n" + e.stackTraceToString())
      )
    }
  }

  suspend fun websocket(request: PureClientRequest): PureChannel {
    val channel = request.channel ?: CompletableDeferred()
    ktorClient.ws(request = { fromPureClientRequest(request) }) {
      val ws = this
      val income = Channel<PureFrame>()
      val outgoing = Channel<PureFrame>()
      val pureChannel = PureChannel(income, outgoing, ws).also {
        channel.complete(it)
      }
      // pureChannel.afterStart() TODO 这里能能不能使用afterStart来阻塞数据的接收？
      ws.pipeToPureChannel(request.href, income, outgoing, pureChannel)
    }
    return channel.await()
  }
}
