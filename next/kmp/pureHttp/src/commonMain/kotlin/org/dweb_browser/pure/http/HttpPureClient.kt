package org.dweb_browser.pure.http

import kotlinx.coroutines.CompletableDeferred

expect class HttpPureClient(config: HttpPureClientConfig = HttpPureClientConfig()) {
  suspend fun fetch(request: PureClientRequest): PureResponse
  suspend fun websocket(request: PureClientRequest): PureChannel
}

class HttpPureClientConfig(
  val httpProxyUrl: String? = null,
  val enableCache: Boolean = false,
  val dwebSsl: DwebSslConfig? = null,
) {
  interface DwebSslConfig {
    val issuerName: String
    val publicKey: ByteArray
  }
}

suspend fun HttpPureClient.fetch(
  url: String,
  method: PureMethod = PureMethod.GET,
  headers: PureHeaders = PureHeaders(),
  body: IPureBody = IPureBody.Empty,
): PureResponse {
  return fetch(PureClientRequest(url, method, headers, body))
}

suspend fun HttpPureClient.websocket(
  url: String,
  subProtocol: List<String>? = null,
  headers: PureHeaders = PureHeaders(),
): PureChannel {
  val request = PureClientRequest(
    href = url,
    method = PureMethod.GET,
    headers = headers.apply {
      if (subProtocol != null) {
        init("Sec-WebSocket-Protocol", subProtocol.joinToString(", "))
      }
    },
    channel = CompletableDeferred()
  )
  return websocket(request)
}

val defaultHttpPureClient = HttpPureClient()