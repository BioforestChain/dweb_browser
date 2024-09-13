package org.dweb_browser.pure.http

import io.ktor.client.engine.darwin.ChallengeHandler
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig
import io.ktor.client.engine.darwin.KtorNSURLSessionDelegate
import io.ktor.http.Url
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.helper.hexString
import org.dweb_browser.pure.http.ktor.KtorPureClient
import org.dweb_browser.pure.http.ktor.toKtorClientConfig
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLCredentialPersistence
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDataDelegateProtocol
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSURLSessionDelegateProtocol
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSURLSessionTaskMetrics
import platform.Foundation.NSURLSessionWebSocketCloseCode
import platform.Foundation.NSURLSessionWebSocketDelegateProtocol
import platform.Foundation.NSURLSessionWebSocketTask
import platform.Foundation.create
import platform.Foundation.serverTrust
import platform.darwin.NSObject

//public class Test: KtorNSURLSessionDelegate {
//
//}

private class CustomChallengerHandler(private val publicKey: ByteArray) : ChallengeHandler {
  private val urlCredential = buildURLCrendentialsWithCertificate()
  override fun invoke(
    session: NSURLSession,
    task: NSURLSessionTask,
    challenge: NSURLAuthenticationChallenge,
    completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit
  ) {
    println("QAQ CustomChallengerHandler url=${task.currentRequest?.URL?.host}")
    if (task.currentRequest?.URL?.host?.endsWith(".dweb") == true) {
      completionHandler(NSURLSessionAuthChallengeUseCredential, urlCredential)
    } else {
      completionHandler(
        NSURLSessionAuthChallengePerformDefaultHandling, challenge.proposedCredential
      )
    }
  }

  @OptIn(BetaInteropApi::class)
  private fun buildURLCrendentialsWithCertificate() = NSURLCredential.create(
    user = publicKey.hexString,
    password = "",
    persistence = NSURLCredentialPersistence.NSURLCredentialPersistenceForSession
  )
}


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
          "HTTPEnable" to 1,
          "HTTPProxy" to host,
          "HTTPPort" to port,
        )
      }

      config.dwebSsl?.also { dwebSsl ->
        val session = NSURLSession.sessionWithConfiguration(
          configuration,
          CustomURLSessionDelegate(ktorDelegate, CustomChallengerHandler(dwebSsl.publicKey)),
          null
        )

        usePreconfiguredSession(
          session, ktorDelegate
//          CustomKtorNSURLSessionDelegateinternalÎ(
//            ktorDelegate,
//            CustomChallengerHandler(dwebSsl.publicKey)
//          ) as KtorNSURLSessionDelegate
        )
      }
    }

  }) {

}


//private class CustomKtorNSURLSessionDelegateinternal(
//  val delegate: KtorNSURLSessionDelegate,
//  private val challengeHandler: ChallengeHandler?
//) : NSObject(), NSURLSessionDataDelegateProtocol,
//  NSURLSessionWebSocketDelegateProtocol by delegate {
//  @ObjCSignatureOverride
//  override fun URLSession(session: NSURLSession, taskIsWaitingForConnectivity: NSURLSessionTask) {
//    delegate.URLSession(
//      session = session,
//      taskIsWaitingForConnectivity
//    )
//  }
//
////  @ObjCSignatureOverride
////  override fun URLSession(session: NSURLSession, didCreateTask: NSURLSessionTask) {
////    delegate.URLSession(session, didCreateTask)
////  }
//
//
//  override fun URLSession(
//    session: NSURLSession,
//    task: NSURLSessionTask,
//    didReceiveChallenge: NSURLAuthenticationChallenge,
//    completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit
//  ) {
//    val handler = challengeHandler
//    if (handler != null) {
//      handler(session, task, didReceiveChallenge, completionHandler)
//    } else {
//      completionHandler(
//        NSURLSessionAuthChallengePerformDefaultHandling,
//        didReceiveChallenge.proposedCredential
//      )
//    }
//  }
//}

@OptIn(ExperimentalForeignApi::class)
private class CustomURLSessionDelegate(
  private val ktorDelegate: KtorNSURLSessionDelegate,
  private val challengeHandler: ChallengeHandler?
) : NSObject(), NSURLSessionDataDelegateProtocol, NSURLSessionWebSocketDelegateProtocol,
  NSURLSessionDelegateProtocol {
  override fun URLSession(
    session: NSURLSession,
    dataTask: NSURLSessionDataTask,
    didReceiveData: NSData
  ) {
    ktorDelegate.URLSession(session = session, dataTask = dataTask, didReceiveData = didReceiveData)
  }

  override fun URLSession(
    session: NSURLSession,
    task: NSURLSessionTask,
    didCompleteWithError: NSError?
  ) {
    ktorDelegate.URLSession(
      session = session,
      task = task,
      didCompleteWithError = didCompleteWithError
    )
  }

  override fun URLSession(
    session: NSURLSession,
    webSocketTask: NSURLSessionWebSocketTask,
    didOpenWithProtocol: String?
  ) {
    ktorDelegate.URLSession(
      session = session,
      webSocketTask = webSocketTask,
      didOpenWithProtocol = didOpenWithProtocol
    )
  }

  override fun URLSession(
    session: NSURLSession,
    webSocketTask: NSURLSessionWebSocketTask,
    didCloseWithCode: NSURLSessionWebSocketCloseCode,
    reason: NSData?
  ) {
    ktorDelegate.URLSession(
      session = session,
      webSocketTask = webSocketTask,
      didCloseWithCode = didCloseWithCode,
      reason = reason,
    )
  }

  override fun URLSession(
    session: NSURLSession,
    didReceiveChallenge: NSURLAuthenticationChallenge,
    completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit
  ) {
    println("QAQ URLSession didReceiveChallenge")
    if (didReceiveChallenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
      NSURLSessionAuthChallengeCancelAuthenticationChallenge
      completionHandler(
        NSURLSessionAuthChallengeUseCredential,
        NSURLCredential.create(trust = didReceiveChallenge.protectionSpace.serverTrust)
      )
    } else {
      completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
    }
  }

  override fun URLSession(
    session: NSURLSession,
    task: NSURLSessionTask,
    didReceiveChallenge: NSURLAuthenticationChallenge,
    completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit
  ) {
    println("QAQ URLSession task didReceiveChallenge")
    val handler = challengeHandler
    if (handler != null) {
      handler(session, task, didReceiveChallenge, completionHandler)
    } else {
      completionHandler(
        NSURLSessionAuthChallengePerformDefaultHandling, didReceiveChallenge.proposedCredential
      )
    }
  }

  override fun URLSession(session: NSURLSession, didCreateTask: NSURLSessionTask) {
    println("QAQ URLSession session: NSURLSession, didCreateTask: NSURLSessionTask")
    super<NSURLSessionDataDelegateProtocol>.URLSession(session, didCreateTask)
  }
}