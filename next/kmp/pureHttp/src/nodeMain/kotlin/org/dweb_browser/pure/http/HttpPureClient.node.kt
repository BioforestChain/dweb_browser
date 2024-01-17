package org.dweb_browser.pure.http

actual class HttpPureClient {
  actual suspend fun fetch(request: PureClientRequest): PureResponse {
    TODO("Not yet implemented")
  }

  actual suspend fun websocket(request: PureClientRequest): PureResponse {
    TODO("Not yet implemented")
  }
}