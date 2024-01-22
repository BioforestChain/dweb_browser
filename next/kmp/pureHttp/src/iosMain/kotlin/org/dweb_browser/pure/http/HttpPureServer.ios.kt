package org.dweb_browser.pure.http

import org.dweb_browser.pure.http.ktor.KtorPureServer

actual class HttpPureServer actual constructor(onRequest: HttpPureServerOnRequest) :
  KtorPureServer(io.ktor.server.cio.CIO, onRequest) {
  init {
    allHttpPureServerInstances.add(this)
  }
}