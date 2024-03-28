package org.dweb_browser.pure.http

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
        serverDeferred.complete(it)
        it.start(wait = false)
      }
    }

    return getPort()
  }
}

