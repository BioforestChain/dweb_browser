package org.dweb_browser.pure.http

import android.annotation.SuppressLint
import okhttp3.OkHttpClient
import org.dweb_browser.helper.Debugger
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

private val debugOkHttp = Debugger("okhttp")

internal object OkHttpUtil {
  @Throws(Exception::class)
  fun init(dwebSsl: HttpPureClientConfig.DwebSslConfig): OkHttpClient {
    debugOkHttp("init", "Initialising httpUtil with default configuration")
    val builder = configureToIgnoreCertificate(OkHttpClient.Builder(), dwebSsl)
    //Other application specific configuration
    return builder.build()
  }

  private fun configureToIgnoreCertificate(
    builder: OkHttpClient.Builder,
    dwebSsl: HttpPureClientConfig.DwebSslConfig,
  ): OkHttpClient.Builder {
    debugOkHttp("configureToIgnoreCertificate", "Ignore Ssl Certificate")
    runCatching {
      val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
      factory.init(null as KeyStore?)
      val standardTrustManager =
        factory.trustManagers.filterIsInstance<X509TrustManager>().firstOrNull()
          ?: throw NoSuchAlgorithmException("No default X509TrustManager found")
      // Create a trust manager that does not validate certificate chains
      val trustAllCert = @SuppressLint("CustomX509TrustManager")
      object : X509TrustManager {
        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
          debugOkHttp.verbose("checkClientTrusted") {
            "authType=$authType chain=${chain.joinToString { it.toString() }}"
          }
          standardTrustManager.checkClientTrusted(chain, authType)
        }

        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
          debugOkHttp.verbose("checkServerTrusted") {
            "authType=$authType chain=${chain.joinToString { it.toString() }}"
          }
          chain.firstOrNull()?.let { cert ->
            when {
              cert.issuerDN.name == dwebSsl.issuerName
                  && cert.publicKey.encoded.contentEquals(
                dwebSsl.publicKey
              ) -> cert

              else -> null
            }
          } ?: standardTrustManager.checkServerTrusted(chain, authType)
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
          debugOkHttp.verbose("getAcceptedIssuers") {
            "default acceptedIssuers=${standardTrustManager.acceptedIssuers.joinToString { it.issuerDN.name }}"
          }
          return standardTrustManager.acceptedIssuers
        }
      }
      // Install the all-trusting trust manager
      val sslContext = SSLContext.getInstance("SSL")
      sslContext.init(null, arrayOf(trustAllCert), SecureRandom())
      // Create an ssl socket factory with our all-trusting manager
      val sslSocketFactory = sslContext.socketFactory

      builder.sslSocketFactory(sslSocketFactory, trustAllCert)
      val defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()
      builder.hostnameVerifier { hostname, session ->
        debugOkHttp("hostnameVerifier") {
          """
            hostname=$hostname
            session.isValid=${session.isValid}
            session.peerHost=${session.peerHost}
            session.peerPort=${session.peerPort}
            session.peerCertificates=${session.peerCertificates.joinToString { it.toString() }}
          """.trimIndent()
        }
        when {
          hostname.endsWith(".dweb") -> true
          else -> defaultHostnameVerifier.verify(hostname, session)
        }
      }
    }.getOrElse { err ->
      debugOkHttp(
        "configureToIgnoreCertificate",
        "Exception while configuring IgnoreSslCertificate",
        err
      )
    }
    return builder
  }
}