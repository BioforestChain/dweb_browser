package org.dweb_browser.pure.http

import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import org.dweb_browser.pure.http.ktor.KtorPureServer

actual class HttpPureServer actual constructor(onRequest: HttpPureServerOnRequest) :
  KtorPureServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>(CIO, onRequest) {
  init {
    allHttpPureServerInstances.add(this)
  }
}