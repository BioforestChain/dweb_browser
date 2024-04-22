package org.dweb_browser.pure.http

import io.ktor.server.engine.sslConnector
import io.ktor.server.jetty.Jetty
import io.ktor.server.jetty.JettyApplicationEngine
import io.ktor.server.jetty.JettyApplicationEngineBase
import org.dweb_browser.pure.http.ktor.KtorPureServer

actual class HttpPureServer actual constructor(onRequest: HttpPureServerOnRequest) :
  KtorPureServer<JettyApplicationEngine, JettyApplicationEngineBase.Configuration>(Jetty, onRequest) {
  init {
    allHttpPureServerInstances.add(this)
  }

  actual override suspend fun start(port: UShort): UShort {
    if (!serverDeferred.isCompleted) {
      createServer({

      }) {
//        connector {
//          this.port = port.toInt()
//          this.host = "0.0.0.0"
//        }

        sslConnector(
          keyStore = SslSettings.keyStore,
          keyAlias = SslSettings.keyAlias,
          keyStorePassword = { SslSettings.keyStorePassword.toCharArray() },
          privateKeyPassword = { SslSettings.privateKeyPassword.toCharArray() }) {
          this.port = port.toInt()
          this.keyStorePath = SslSettings.keyStoreFile
        }
      }.also {
        it.start(wait = false)
        serverDeferred.complete(it)
      }
    }

    return getPort()
  }
}

