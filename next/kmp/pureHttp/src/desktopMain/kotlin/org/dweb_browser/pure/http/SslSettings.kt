package org.dweb_browser.pure.http

import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.certificates.saveToFile
import org.dweb_browser.helper.randomUUID
import java.io.File
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object SslSettings {

  val keyStoreFile =
    File(System.getProperty("user.home")).resolve(".dweb/pure-http/reverse-proxy.keystore.jks")
  val keyAlias = "dwebBrowserReverseProxy"
  val keyStorePassword = randomUUID()
  val privateKeyPassword = randomUUID()
  val keyStore = buildKeyStore {
    certificate(keyAlias) {
      password = privateKeyPassword
      domains = listOf("*.dweb")
    }
  }.also {
    it.saveToFile(keyStoreFile, keyStorePassword)
  }

  fun getTrustManagerFactory(): TrustManagerFactory? {
    val trustManagerFactory =
      TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    trustManagerFactory.init(keyStore)
    return trustManagerFactory
  }

  fun getSslContext(): SSLContext {
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, mutableListOf<TrustManager>().run {
      getTrustManagerFactory()?.trustManagers?.also {
        println("trustManagers=${it.joinToString(", ")}")
        addAll(it)
      }
      toTypedArray()
    }, null)
    return sslContext
  }

  val trustManager by lazy {
    getTrustManagerFactory()?.trustManagers?.first { it is X509TrustManager } as X509TrustManager
  }

}