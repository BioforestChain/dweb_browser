package org.dweb_browser.microservice.sys.dns

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareRequest
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.toLowerCasePreservingASCIIRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.httpFetcher
import org.dweb_browser.helper.printDebug
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.help.AdapterManager
import org.dweb_browser.microservice.help.canReadContent
import org.dweb_browser.microservice.help.toHttpRequestBuilder
import org.dweb_browser.microservice.help.toPureResponse
import org.dweb_browser.microservice.http.PureBinaryBody
import org.dweb_browser.microservice.http.PureRequest
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.http.PureStreamBody
import org.dweb_browser.microservice.http.PureStringBody
import org.dweb_browser.microservice.ipc.helper.IpcHeaders
import org.dweb_browser.microservice.ipc.helper.IpcMethod

typealias FetchAdapter = suspend (remote: MicroModule, request: PureRequest) -> PureResponse?

fun debugFetch(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("fetch", tag, msg, err)

fun debugFetchFile(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("fetch-file", tag, msg, err)

/**
 * file:/// => /usr & /sys as const
 * file://file.sys.dweb/ => /home & /tmp & /share as userData
 */
val nativeFetchAdaptersManager = NativeFetchAdaptersManager()

class NativeFetchAdaptersManager : AdapterManager<FetchAdapter>() {

  private var client = httpFetcher

  fun setClientProvider(client: HttpClient) {
    this.client = client
  }

  class HttpFetch(val manager: NativeFetchAdaptersManager) {
    val client get() = manager.client
    suspend operator fun invoke(request: PureRequest) = fetch(request)
    suspend operator fun invoke(url: String) =
      fetch(PureRequest(method = IpcMethod.GET, href = url))

    suspend fun fetch(request: PureRequest): PureResponse {
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
          debugFetch("httpFetch prepareRequest", request.href)
          try {
            client.prepareRequest(request.toHttpRequestBuilder()).execute {
              debugFetch("httpFetch execute", request.href)
              val byteChannel = it.bodyAsChannel()
              val response = it.toPureResponse(body = PureStreamBody(byteChannel))
              debugFetch("httpFetch response", request.href)
              responsePo.resolve(response)
              while (byteChannel.canReadContent()) {
                delay(1000)
              }
              debugFetch("httpFetch end", request.href)
            }
          } catch (e: Throwable) {
            responsePo.reject(e)
          }
        }
        return responsePo.waitPromise().also {
          debugFetch("httpFetch return", request.href)
        }
      } catch (e: Throwable) {
        debugFetch("httpFetch Throwable", e.message)
        return PureResponse(
          HttpStatusCode.ServiceUnavailable,
          body = PureStringBody(e.stackTraceToString())
        )
      }
    }
  }

  val httpFetch = HttpFetch(this)
}

val httpFetch = nativeFetchAdaptersManager.httpFetch

suspend fun MicroModule.nativeFetch(request: PureRequest): PureResponse {
  for (fetchAdapter in nativeFetchAdaptersManager.adapters) {
    val response = fetchAdapter(this, request)
    if (response != null) {
      return response
    }
  }
  return nativeFetchAdaptersManager.httpFetch(request)
}

suspend inline fun MicroModule.nativeFetch(url: Url) =
  nativeFetch(PureRequest(url.toString(), IpcMethod.GET))

suspend inline fun MicroModule.nativeFetch(url: String) =
  nativeFetch(PureRequest(url, IpcMethod.GET))
