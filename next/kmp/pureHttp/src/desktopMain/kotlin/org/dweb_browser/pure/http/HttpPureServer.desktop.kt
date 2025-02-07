package org.dweb_browser.pure.http

import io.ktor.server.engine.connector
import io.ktor.server.engine.sslConnector
import io.ktor.server.jetty.jakarta.Jetty
import io.ktor.server.jetty.jakarta.JettyApplicationEngine
import io.ktor.server.jetty.jakarta.JettyApplicationEngineBase
import org.dweb_browser.pure.http.ktor.KtorPureServer

actual class HttpPureServer actual constructor(onRequest: HttpPureServerOnRequest) :
  KtorPureServer<JettyApplicationEngine, JettyApplicationEngineBase.Configuration>(
    Jetty,
    onRequest
  ) {
  init {
    allHttpPureServerInstances.add(this)
  }

  actual override suspend fun start(port: UShort): UShort {
    return start(port, true)
  }

  suspend fun start(port: UShort, https: Boolean) = startServer {
    createServer({
      if (https) {
        sslConnector(
          keyStore = SslSettings.keyStore,
          keyAlias = SslSettings.keyAlias,
          keyStorePassword = { SslSettings.keyStorePassword.toCharArray() },
          privateKeyPassword = { SslSettings.privateKeyPassword.toCharArray() }) {
          this.port = port.toInt()
          this.keyStorePath = SslSettings.keyStoreFile
        }
      } else {
        connector {
          this.port = port.toInt()
          this.host = "0.0.0.0"
        }
      }
    })
  }
}

