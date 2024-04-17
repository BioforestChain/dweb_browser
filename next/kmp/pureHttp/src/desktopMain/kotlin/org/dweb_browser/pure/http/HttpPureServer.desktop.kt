package org.dweb_browser.pure.http

import io.ktor.server.engine.connector
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import org.dweb_browser.pure.http.ktor.KtorPureServer

actual class HttpPureServer actual constructor(onRequest: HttpPureServerOnRequest) :
  KtorPureServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>(Netty, onRequest) {
  init {
    allHttpPureServerInstances.add(this)
  }

  actual override suspend fun start(port: UShort): UShort {
    return start(port, true)
  }

  suspend fun start(port: UShort, https: Boolean): UShort {
    if (!serverDeferred.isCompleted) {
      createServer({
      }) {
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
      }.also {
        serverDeferred.complete(it)
        it.start(wait = false)
      }
    }

    return getPort()
  }
}

