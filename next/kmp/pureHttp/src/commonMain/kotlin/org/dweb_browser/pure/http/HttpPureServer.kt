package org.dweb_browser.pure.http

typealias HttpPureServerOnRequest = suspend (PureServerRequest) -> PureResponse

expect class HttpPureServer(onRequest: HttpPureServerOnRequest) {
  suspend fun start(port: UShort): UShort
  suspend fun close()
}