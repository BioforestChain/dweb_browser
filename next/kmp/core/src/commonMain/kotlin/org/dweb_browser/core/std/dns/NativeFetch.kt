package org.dweb_browser.core.std.dns

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.ws
import io.ktor.client.request.prepareRequest
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.toLowerCasePreservingASCIIRules
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.AdapterManager
import org.dweb_browser.core.help.toHttpRequestBuilder
import org.dweb_browser.core.help.toPureResponse
import org.dweb_browser.core.http.PureBinaryBody
import org.dweb_browser.core.http.PureClientRequest
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.PureStream
import org.dweb_browser.core.http.PureStreamBody
import org.dweb_browser.core.http.PureStringBody
import org.dweb_browser.core.ipc.helper.IpcHeaders
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.httpFetcher

typealias FetchAdapter = suspend (remote: MicroModule, request: PureClientRequest) -> PureResponse?

val debugFetch = Debugger("fetch")

val debugFetchFile = Debugger("fetch-file")

/**
 * file:/// => /usr & /sys as const
 * file://file.std.dweb/ => /home & /tmp & /share as userData
 */
val nativeFetchAdaptersManager = NativeFetchAdaptersManager()

class NativeFetchAdaptersManager : AdapterManager<FetchAdapter>() {

  private var client = httpFetcher

  fun setClientProvider(client: HttpClient) {
    this.client = client
  }

  class HttpFetch(private val manager: NativeFetchAdaptersManager) {
    val client get() = manager.client
    suspend operator fun invoke(request: PureClientRequest) = fetch(request)
    suspend operator fun invoke(url: String, method: IpcMethod = IpcMethod.GET) =
      fetch(PureClientRequest(method = method, href = url))

    suspend fun fetch(request: PureClientRequest): PureResponse {
      try {
        debugFetch("httpFetch request", request.href)

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
                    headers = IpcHeaders().apply { set("Content-Type", meta) },
                    body = PureStringBody(bodyContent)
                  )
                }

                2 -> {
                  val encoding = metaInfo[1]
                  return if (encoding.trim().toLowerCasePreservingASCIIRules() == "base64") {
                    PureResponse(
                      HttpStatusCode.OK,
                      headers = IpcHeaders().apply { set("Content-Type", metaInfo[0]) },
                      body = PureBinaryBody(bodyContent.decodeBase64Bytes())
                    )
                  } else {
                    PureResponse(
                      HttpStatusCode.OK,
                      headers = IpcHeaders().apply { set("Content-Type", meta) },
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
        val responsePo = PromiseOut<PureResponse>()
        CoroutineScope(ioAsyncExceptionHandler).launch {
          try {
            client.prepareRequest(request.toHttpRequestBuilder()).execute {
              debugFetch("httpFetch execute", request.href)
              val byteChannel = it.bodyAsChannel()
              val pureStream = PureStream(byteChannel)
              val onClosePo = CompletableDeferred<Unit>()
              pureStream.onClose {
                onClosePo.complete(Unit)
              }
              val response = it.toPureResponse(body = PureStreamBody(pureStream))
              debugFetch("httpFetch response", request.href)
              responsePo.resolve(response)
              onClosePo.await()
            }
            // 线程里面的错误需要在线程里捕捉
          } catch (e: Throwable) {
            // TODO 连接超时提示用户
            debugFetch("httpFetch error", e.stackTraceToString())
            val response = PureResponse(
              HttpStatusCode.ServiceUnavailable,
              body = PureStringBody(request.url.toString() + "\n" + e.stackTraceToString())
            )
            responsePo.resolve(response)
          }
        }
        return responsePo.waitPromise()
      } catch (e: Throwable) {
        debugFetch("httpFetch", request.url, e)
        return PureResponse(
          HttpStatusCode.ServiceUnavailable,
          body = PureStringBody(request.url.toString() + "\n" + e.stackTraceToString())
        )
      }
    }
  }


  class HttpWebSocket(private val manager: NativeFetchAdaptersManager) {
    val client get() = manager.client
    suspend fun webSocket(request: PureClientRequest): PureResponse {
      client.ws {
      }
      TODO("")
    }
  }

  val httpFetch = HttpFetch(this)
  val httpWebSocket = HttpWebSocket(this)
}

val httpFetch = nativeFetchAdaptersManager.httpFetch
val httpWebSocket = nativeFetchAdaptersManager.httpWebSocket

suspend fun MicroModule.nativeFetch(request: PureClientRequest): PureResponse {
  for (fetchAdapter in nativeFetchAdaptersManager.adapters) {
    val response = fetchAdapter(this, request)
    if (response != null) {
      return response
    }
  }
  return nativeFetchAdaptersManager.httpFetch(request)
}

suspend inline fun MicroModule.nativeFetch(url: Url) = nativeFetch(IpcMethod.GET, url)

suspend inline fun MicroModule.nativeFetch(url: String) = nativeFetch(IpcMethod.GET, url)

suspend inline fun MicroModule.nativeFetch(method: IpcMethod, url: Url) =
  nativeFetch(PureClientRequest(url.toString(), method))

suspend inline fun MicroModule.nativeFetch(method: IpcMethod, url: String) =
  nativeFetch(PureClientRequest(url, method))
