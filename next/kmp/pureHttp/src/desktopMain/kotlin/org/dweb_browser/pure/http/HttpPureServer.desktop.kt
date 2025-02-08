package org.dweb_browser.pure.http

import io.ktor.server.engine.connector
import io.ktor.server.engine.sslConnector
import io.ktor.server.jetty.jakarta.Jetty
import io.ktor.server.jetty.jakarta.JettyApplicationEngine
import io.ktor.server.jetty.jakarta.JettyApplicationEngineBase
import org.dweb_browser.pure.http.ktor.KtorPureServer
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.SecureRequestCustomizer
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector

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

  suspend fun start(port: UShort, https: Boolean): UShort {
    lateinit var jettyServer: Server
    return startServer(
      onStarted = {
        // jetty.jakarta 的 configureServer 调用的时机变了，所以这里只能挪到这里来修改 SNI 相关的配置
        jettyServer.connectors.firstOrNull { it is ServerConnector }
          ?.also { sslServerConnector ->
            val connectionFactory =
              sslServerConnector.getConnectionFactory(HttpConnectionFactory::class.java)
            if (connectionFactory != null) {
              val secureRequestCustomizer = connectionFactory.httpConfiguration
                .getCustomizer(SecureRequestCustomizer::class.java)
              // 详见 https://github.com/jetty/jetty.project/blob/jetty-12.0.x/jetty-core/jetty-server/src/main/java/org/eclipse/jetty/server/SecureRequestCustomizer.java#L228-L232 这里的配置要求
              if (secureRequestCustomizer != null) {
                secureRequestCustomizer.isSniHostCheck = false
                secureRequestCustomizer.isSniRequired = false
              }
            }
          }
      },
      createServer = {
        val serverEngine = createServer(config = {
          if (https) {
            sslConnector(
              keyStore = SslSettings.keyStore,
              keyAlias = SslSettings.keyAlias,
              keyStorePassword = { SslSettings.keyStorePassword.toCharArray() },
              privateKeyPassword = { SslSettings.privateKeyPassword.toCharArray() }) {
              this.port = port.toInt()
              this.host = "0.0.0.0"
              this.keyStorePath = SslSettings.keyStoreFile
            }
            configureServer = {
              jettyServer = server
            }
          } else {
            connector {
              this.port = port.toInt()
              this.host = "0.0.0.0"
            }
          }
        })
        serverEngine
      },
    )
  }
}

