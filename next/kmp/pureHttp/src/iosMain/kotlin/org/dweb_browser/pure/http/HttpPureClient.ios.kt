package org.dweb_browser.pure.http

import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig
import io.ktor.client.engine.darwin.KtorNSURLSessionDelegate
import io.ktor.http.Url
import org.dweb_browser.pure.http.ktor.KtorPureClient
import org.dweb_browser.pure.http.ktor.toKtorClientConfig
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration

actual class HttpPureClient actual constructor(config: HttpPureClientConfig) :
  KtorPureClient<DarwinClientEngineConfig>(Darwin, {
    config.toKtorClientConfig<DarwinClientEngineConfig>()()

    engine {
      configureRequest {
        setRequiresDNSSECValidation(false)
      }

      val ktorDelegate = KtorNSURLSessionDelegate()

      val configuration = NSURLSessionConfiguration.defaultSessionConfiguration()
      config.httpProxyUrl?.let { Url(it) }?.apply {
        configuration.connectionProxyDictionary = mapOf<Any?, Any?>(
          "HTTPSEnable" to 1,
          "HTTPSProxy" to host,
          "HTTPSPort" to port,
        )
      }

      config.dwebSsl?.also { dwebSsl ->
        val session = NSURLSession.sessionWithConfiguration(
          configuration, KtorNSURLSessionDelegateWrapper(ktorDelegate, dwebSsl), null
        )

        usePreconfiguredSession(session, ktorDelegate)

//        handleChallenge { session, task, challenge, completionHandler ->
//          NSSessionUtils.didReceiveChallenge(dwebSsl, session, challenge, completionHandler)
//        }
      }
    }

  }) {
}
