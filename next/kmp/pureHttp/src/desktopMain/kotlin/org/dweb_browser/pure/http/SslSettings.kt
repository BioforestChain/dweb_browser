package org.dweb_browser.pure.http

import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.certificates.saveToFile
import org.dweb_browser.helper.randomUUID
import java.io.File
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
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

  // JVM默认信任证书
  private val defaultTrustManager: X509TrustManager by lazy {
    val trustManagerFactory =
      TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    trustManagerFactory.init(null as KeyStore?)
    trustManagerFactory.trustManagers.first { it is X509TrustManager } as X509TrustManager
  }

  private fun getTrustManagerFactory(): TrustManagerFactory? {
    val trustManagerFactory =
      TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    trustManagerFactory.init(keyStore)
    return trustManagerFactory
  }

  // 必须添加JVM默认信任证书和自签证书，否则会出现 httpStatusCode: 417 Expectation Failed
  @Suppress("CustomX509TrustManager")
  private class CompositeX509TrustManager(
    val primaryTrustManager: X509TrustManager?,
    val secondaryTrustManager: X509TrustManager?,
  ) : X509TrustManager {

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {
      try {
        primaryTrustManager?.checkClientTrusted(chain, authType)
      } catch (ex: CertificateException) {
        secondaryTrustManager?.checkClientTrusted(chain, authType)
      }
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
      try {
        primaryTrustManager?.checkServerTrusted(chain, authType)
      } catch (ex: CertificateException) {
        secondaryTrustManager?.checkServerTrusted(chain, authType)
      }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> =
      (primaryTrustManager?.acceptedIssuers.orEmpty()
        .toList() + secondaryTrustManager?.acceptedIssuers.orEmpty().toList()).toTypedArray()
  }

  fun getSslContext(): SSLContext {
    val sslContext = SSLContext.getInstance("TLS")
    val trustManager = CompositeX509TrustManager(defaultTrustManager, trustManager)
    sslContext.init(null, arrayOf(trustManager), null)
    return sslContext
  }

  val trustManager by lazy {
    getTrustManagerFactory()?.trustManagers?.first { it is X509TrustManager } as X509TrustManager
  }

}