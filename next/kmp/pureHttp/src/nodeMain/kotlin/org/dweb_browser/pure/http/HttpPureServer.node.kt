package org.dweb_browser.pure.http

actual class HttpPureServer actual constructor(actual val onRequest: HttpPureServerOnRequest) {
  init {
    allHttpPureServerInstances.add(this)
  }

  actual suspend fun start(port: UShort): UShort {
    TODO("Not yet implemented")
  }

  actual suspend fun close() {
  }
}