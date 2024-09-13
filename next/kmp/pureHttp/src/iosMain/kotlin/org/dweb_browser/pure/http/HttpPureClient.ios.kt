package org.dweb_browser.pure.http

import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig
import io.ktor.client.engine.darwin.KtorNSURLSessionDelegate
import io.ktor.http.Url
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.pure.http.ktor.KtorPureClient
import org.dweb_browser.pure.http.ktor.toKtorClientConfig
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.create
import platform.Foundation.serverTrust

actual class HttpPureClient actual constructor(config: HttpPureClientConfig) :
  KtorPureClient<DarwinClientEngineConfig>(Darwin, {
    config.toKtorClientConfig<DarwinClientEngineConfig>()()

    engine {

      if (false) {

        val ktorDelegate = KtorNSURLSessionDelegate()
//      NSURLSession().configuration =  NSURLSessionConfiguration.defaultSessionConfiguration
//      val sessionBuilder: (NSURLSessionDelegateProtocol) -> NSURLSession = get()
//      val session = sessionBuilder(ktorDelegate.g)
        val session = NSURLSession.sessionWithConfiguration(
          configuration = NSURLSessionConfiguration.defaultSessionConfiguration,
        )
        usePreconfiguredSession(session, ktorDelegate)
      }
//      usePreconfiguredSession()
      @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
      handleChallenge { session, task, challenge, completionHandler ->
        println("QAQ handleChallenge ${task.currentRequest?.URL()?.toString()}")
        if (challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
          val x: NSURLSessionAuthChallengeDisposition
          completionHandler(
            NSURLSessionAuthChallengeUseCredential,
            NSURLCredential.create(trust = challenge.protectionSpace.serverTrust)
          )
        } else {
          completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
        }
      }
    }
  }) {

}