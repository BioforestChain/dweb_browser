package org.dweb_browser.core.std.websocketClient

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.ws
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.invoke
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.utils.io.reader
import io.ktor.websocket.send
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.DwebResult
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.httpFetch
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.consumeEachArrayRange
import org.dweb_browser.helper.toJsonElement

class WebSocketClientNMM : NativeMicroModule("websocket-client.std.dweb", "WebSocket Client") {
  init {
    short_name = "websocket-client"
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Protocol_Service)
  }

  private val websocketsMap = mutableMapOf<String, DefaultClientWebSocketSession>()

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
//    nativeFetchAdaptersManager.append { fromMM, request ->
//      if (request.url.protocol == URLProtocol.WS) {
//        httpFetch.client.ws({
//          takeFrom(
//            HttpRequestBuilder(
//              scheme = URLProtocol.WS.name,
//              host = request.url.host,
//              port = request.url.port,
//              path = request.url.encodedPathAndQuery
//            )
//          )
//        }) {
//          this
//        }
//        null
//      } else if (request.url.protocol == URLProtocol.WSS) {
//        httpFetch.client.wss({
//          takeFrom(
//            HttpRequestBuilder(
//              scheme = URLProtocol.WSS.name,
//              host = request.url.host,
//              port = request.url.port,
//              path = request.url.encodedPathAndQuery
//            )
//          )
//        }) {}
//        null
//      } else null
//    }

    routes(
      "/connect" bind HttpMethod.Get by defineJsonResponse {
        val url = Url(request.query("url"))
        val protocol = URLProtocol.byName[url.protocol.name]

        if(protocol != URLProtocol.WS && protocol != URLProtocol.WSS) {
          return@defineJsonResponse DwebResult(false, "websocket only support ws or wss.").toJsonElement()
        }

        val deferred = CompletableDeferred<DefaultClientWebSocketSession>()
        httpFetch.client.ws({
          takeFrom(
            HttpRequestBuilder(
              scheme = protocol.name,
              host = url.host,
              port = url.port,
              path = url.encodedPathAndQuery
            )
          )
        }) {
          deferred.complete(this)
        }

        val session = deferred.await()
        val sessionId = UUID()
        websocketsMap[sessionId] = session

        DwebResult(true, sessionId).toJsonElement()
      },
      "/onMessage" bind HttpMethod.Get by defineJsonLineResponse {
        val sessionId = request.query("sessionId")
        val session = websocketsMap[sessionId]!!
        
        session.reader {
          channel.consumeEachArrayRange { byteArray, _ ->
            emit(byteArray)
          }
        }
      },
      "/send" bind HttpMethod.Post by defineEmptyResponse {
        val sessionId = request.query("sessionId")
        val session = websocketsMap[sessionId]!!
        val reader = request.body.toPureStream().getReader("websocket client: $sessionId")

        reader.consumeEachArrayRange { byteArray, _ ->
          session.send(byteArray)
        }
      },
      "/close" bind HttpMethod.Get by defineBooleanResponse {
        websocketsMap.remove(request.query("sessionId"))?.cancel()

        true
      }
    )
  }

  override suspend fun _shutdown() {}
}