package org.dweb_browser.pure.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.prepareRequest
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ioAsyncExceptionHandler

val debugHttpPureClient = Debugger("httpPureClient")

var ktorHttpClient: HttpClient? = null
  get() = field ?: throw Throwable("require setupKtorHttpClient")
  private set(value) {
    field = value
  }

fun setupKtorHttpClient(client: HttpClient) {
  ktorHttpClient = client
}

fun setupKtorHttpClient(engine: HttpClientEngineFactory<*>) {
  setupKtorHttpClient(HttpClient(engine) {
    install(ContentEncoding)
    install(WebSockets)
  })
}

suspend fun ktorPureClient(request: PureClientRequest): PureResponse {
  try {
    debugHttpPureClient("httpFetch request", request.href)
    val client = ktorHttpClient;
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
        client.prepareRequest(request.toHttpRequestBuilder()).execute {
          debugHttpPureClient("httpFetch execute", request.href)
          val byteChannel = it.bodyAsChannel()
          val pureStream = PureStream(byteChannel)
          val onClosePo = CompletableDeferred<Unit>()
          pureStream.onClose {
            onClosePo.complete(Unit)
          }
          val response = it.toPureResponse(body = PureStreamBody(pureStream))
          debugHttpPureClient("httpFetch response", request.href)
          responsePo.resolve(response)
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
        responsePo.resolve(response)
      }
    }
    return responsePo.waitPromise()
  } catch (e: Throwable) {
    debugHttpPureClient("httpFetch", request.url, e)
    return PureResponse(
      HttpStatusCode.ServiceUnavailable,
      body = PureStringBody(request.url.toString() + "\n" + e.stackTraceToString())
    )
  }
}
