package org.dweb_browser.pure.http

actual class HttpPureServer {
  actual suspend fun start(port: UShort, onRequest: HttpPureServerOnRequest) {

  }

  actual suspend fun getPort(): UShort {
    TODO("Not yet implemented")
  }

  actual suspend fun close() {
  }
}