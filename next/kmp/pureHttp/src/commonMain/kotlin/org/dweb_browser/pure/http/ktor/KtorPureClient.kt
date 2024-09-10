package org.dweb_browser.pure.http.ktor

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
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
import org.dweb_browser.helper.globalIoScope
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.pure.http.HttpPureClientConfig
import org.dweb_browser.pure.http.PureChannel
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureFrame
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.pure.http.PureStreamBody
import org.dweb_browser.pure.http.PureStringBody
import org.dweb_browser.pure.http.dataUriToPureResponse
import org.dweb_browser.pure.http.tryDoHttpPureServerResponse

val debugHttpPureClient = Debugger("httpPureClient")

open class KtorPureClient<out T : HttpClientEngineConfig>(
  engine: HttpClientEngineFactory<T>, config: HttpClientConfig<T>.() -> Unit = {},
) {
  val ktorClient = HttpClient(engine) {
    install(HttpTimeout) {
      connectTimeoutMillis = 30_000L
    }
    install(ContentEncoding)
    install(WebSockets)
    config()
  }

  suspend fun fetch(request: PureClientRequest): PureResponse {
    debugHttpPureClient("httpFetch request", request.href)
    if (request.url.protocol.name == "data") {
      return dataUriToPureResponse(request)
    }

    /// 尝试从内部 HttpPureServer 上消化掉请求
    /// 如果请求强行要求通往 dwebproxy ，那么就不走内部 HttpPureServer
    if (request.headers.getOrNull("Sec-Fetch-Dest") != "dwebproxy") {
      tryDoHttpPureServerResponse(request.toServer())?.also {
        return it
      }
    }

    /// 请求标准网络
    val responsePo = CompletableDeferred<PureResponse>()
    globalIoScope.launch {
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
          debugHttpPureClient("httpFetch response") {
            "[${response.status}]${request.href}"
          }
          responsePo.complete(response)
          onClosePo.await()
        }
        // 线程里面的错误需要在线程里捕捉
      } catch (e: Throwable) {
        // TODO 连接超时提示用户
        debugHttpPureClient("httpFetch error", "href:${request.href} ${e.message}")
        val response = PureResponse(
          HttpStatusCode.ServiceUnavailable,
          body = PureStringBody(request.url.toString() + "\n" + e.message)
        )
        responsePo.complete(response)
      }
    }
    return responsePo.await()
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

fun <T : HttpClientEngineConfig> HttpPureClientConfig.toKtorClientConfig(): HttpClientConfig<T>.() -> Unit =
  {
    engine {
      httpProxyUrl?.also { url ->
        proxy = ProxyBuilder.http(url)
      }
    }

    enableCache.trueAlso {
      install(HttpCache)
    }
  }