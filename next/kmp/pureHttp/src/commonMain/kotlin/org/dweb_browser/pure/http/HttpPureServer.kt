package org.dweb_browser.pure.http

typealias HttpPureServerOnRequest = suspend (PureServerRequest) -> PureResponse?

expect class HttpPureServer(onRequest: HttpPureServerOnRequest) {
  val onRequest: HttpPureServerOnRequest
  suspend fun start(port: UShort): UShort
  suspend fun close()
}

internal val allHttpPureServerInstances = mutableSetOf<HttpPureServer>()
internal suspend fun tryDoHttpPureServerResponse(pureServerRequest: PureServerRequest): PureResponse? {
  for (server in allHttpPureServerInstances) {
    val response = server.onRequest(pureServerRequest)
    if (response != null) {
      return response
    }
  }
  return null
}
