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
        return dataUriToPureResponse(request)
      }

      /// 尝试从内部 HttpPureServer 上消化掉请求
      tryDoHttpPureServerResponse(request.toServer())

      /// 请求标准网络
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
    var pureClientRequest = request
    val channel = request.channel ?: CompletableDeferred<PureChannel>().also {
      pureClientRequest = request.copy(channel = it)
    }
    val income = Channel<PureFrame>()
    val outgoing = Channel<PureFrame>()
    val pureChannel = PureChannel(income, outgoing)

    /// 尝试从内部 HttpPureServer 上消化掉请求
    if (tryDoHttpPureServerResponse(pureClientRequest.toServer()) != null) {
      channel.complete(pureChannel)
    } else {
      CoroutineScope(ioAsyncExceptionHandler).launch {
        ktorClient.ws(request = { fromPureClientRequest(request) }) {
          val ws = this
          pureChannel.from = ws
          channel.complete(pureChannel)
          // pureChannel.afterStart() TODO 这里能能不能使用afterStart来阻塞数据的接收？
          pipeToPureChannel(ws, request.href, income, outgoing, pureChannel)
        }
      }
    }

    return channel.await()
  }
}