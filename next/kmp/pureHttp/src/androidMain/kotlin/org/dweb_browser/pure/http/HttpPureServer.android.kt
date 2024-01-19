package org.dweb_browser.pure.http

actual class HttpPureServer actual constructor(onRequest: HttpPureServerOnRequest) :
  KtorPureServer(io.ktor.server.cio.CIO, onRequest) {
  init {
    allHttpPureServerInstances.add(this)
  }
}