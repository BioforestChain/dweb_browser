package org.dweb_browser.dwebview

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.core.module.MicroModule
import platform.CoreGraphics.CGRect
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.WebKit.WKDownload
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigationResponse
import platform.WebKit.WKNavigationResponsePolicy
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebpagePreferences
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class DWebViewEngine(
  frame: CValue<CGRect>,
  remoteMM: MicroModule,
  options: DWebViewOptions,
  configuration: WKWebViewConfiguration,
) : WKWebView(frame, configuration) {
  fun loadUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: throw Exception("invalid url: ${url}")
    val navigation = super.loadRequest(NSURLRequest.requestWithURL(nsUrl))
      ?: throw Exception("fail to get WKNavigation when loadRequest")

  }

  init {
    setNavigationDelegate(
      object : NSObject(), WKNavigationDelegateProtocol {
//        override fun webView(
//          webView: WKWebView,
//          didFailNavigation: WKNavigation?,
//          withError: NSError
//        ) {
//          val superCaller = DNavigationDelegateProtocol.SuperCaller {
//            super.webView(
//              webView,
//              didFailNavigation,
//              withError
//            )
//          }
//          navigationDelegateProtocols.firstNotNullOfOrNull { it.didFailNavigation }
//            ?.invoke(superCaller, webView, didFailNavigation, withError) ?: superCaller()
//        }

//        override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
//          val superCaller = DNavigationDelegateProtocol.SuperCaller {
//            super.webView(webView, didFinishNavigation)
//          }
//          navigationDelegateProtocols.firstNotNullOfOrNull { it.didFinishNavigation }
//            ?.invoke(superCaller, webView, didFinishNavigation) ?: superCaller()
//        }

//        override fun webView(
//          webView: WKWebView,
//          authenticationChallenge: NSURLAuthenticationChallenge,
//          shouldAllowDeprecatedTLS: (Boolean) -> Unit
//        ) {
//          val superCaller = DNavigationDelegateProtocol.SuperCaller {
//            super.webView(
//              webView,
//              authenticationChallenge,
//              shouldAllowDeprecatedTLS
//            )
//          }
//          navigationDelegateProtocols.firstNotNullOfOrNull { it.authenticationChallenge }
//            ?.invoke(superCaller, webView, authenticationChallenge, shouldAllowDeprecatedTLS)
//            ?: superCaller()
//        }
//
//        override fun webView(
//          webView: WKWebView,
//          decidePolicyForNavigationAction: WKNavigationAction,
//          decisionHandler: (WKNavigationActionPolicy) -> Unit
//        ) {
//          val superCaller = DNavigationDelegateProtocol.SuperCaller {
//            super.webView(
//              webView,
//              decidePolicyForNavigationAction,
//              decisionHandler
//            )
//          }
//          navigationDelegateProtocols.firstNotNullOfOrNull { it.decidePolicyForNavigationAction }
//            ?.invoke(superCaller, webView, decidePolicyForNavigationAction, decisionHandler)
//            ?: superCaller()
//        }
//
//        override fun webView(
//          webView: WKWebView,
//          decidePolicyForNavigationResponse: WKNavigationResponse,
//          decisionHandler: (WKNavigationResponsePolicy) -> Unit
//        ) {
//          val superCaller = DNavigationDelegateProtocol.SuperCaller {
//            super.webView(
//              webView,
//              decidePolicyForNavigationResponse,
//              decisionHandler
//            )
//          }
//          navigationDelegateProtocols.firstNotNullOfOrNull { it.decidePolicyForNavigationResponse }
//            ?.invoke(superCaller, webView, decidePolicyForNavigationResponse, decisionHandler)
//            ?: superCaller()
//        }
//
//        override fun webView(
//          webView: WKWebView,
//          didReceiveAuthenticationChallenge: NSURLAuthenticationChallenge,
//          completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit
//        ) {
//          val superCaller = DNavigationDelegateProtocol.SuperCaller {
//            super.webView(
//              webView,
//              didReceiveAuthenticationChallenge,
//              completionHandler
//            )
//          }
//          navigationDelegateProtocols.firstNotNullOfOrNull { it.didReceiveAuthenticationChallenge }
//            ?.invoke(superCaller, webView, didReceiveAuthenticationChallenge, completionHandler)
//            ?: superCaller()
//        }
//
//        override fun webViewWebContentProcessDidTerminate(webView: WKWebView) {
//          val superCaller = DNavigationDelegateProtocol.SuperCaller {
//            super.webViewWebContentProcessDidTerminate(
//              webView
//            )
//          }
//          navigationDelegateProtocols.firstNotNullOfOrNull { it.webViewWebContentProcessDidTerminate }
//            ?.invoke(superCaller, webView) ?: superCaller()
//        }
//
//        override fun webView(
//          webView: WKWebView,
//          navigationAction: WKNavigationAction,
//          didBecomeDownload: WKDownload
//        ) {
//          val superCaller = DNavigationDelegateProtocol.SuperCaller {
//            super.webView(
//              webView,
//              navigationAction,
//              didBecomeDownload
//            )
//          }
//          navigationDelegateProtocols.firstNotNullOfOrNull { it.navigationAction }
//            ?.invoke(superCaller, webView, navigationAction, didBecomeDownload) ?: superCaller()
//        }
//
//        override fun webView(
//          webView: WKWebView,
//          navigationResponse: WKNavigationResponse,
//          didBecomeDownload: WKDownload
//        ) {
//          val superCaller = DNavigationDelegateProtocol.SuperCaller {
//            super.webView(
//              webView,
//              navigationResponse,
//              didBecomeDownload
//            )
//          }
//          navigationDelegateProtocols.firstNotNullOfOrNull { it.navigationResponse }
//            ?.invoke(superCaller, webView, navigationResponse, didBecomeDownload) ?: superCaller()
//        }
//
//        override fun webView(
//          webView: WKWebView,
//          decidePolicyForNavigationAction: WKNavigationAction,
//          preferences: WKWebpagePreferences,
//          decisionHandler: (WKNavigationActionPolicy, WKWebpagePreferences?) -> Unit
//        ) {
//          val superCaller = DNavigationDelegateProtocol.SuperCaller {
//            super.webView(
//              webView,
//              decidePolicyForNavigationAction,
//              preferences,
//              decisionHandler
//            )
//          }
//          navigationDelegateProtocols.firstNotNullOfOrNull { it.decidePolicyForNavigationActionPreferences }
//            ?.invoke(
//              superCaller,
//              webView,
//              decidePolicyForNavigationAction,
//              preferences,
//              decisionHandler
//            )
//            ?: superCaller
//        }
      })
  }

  private val navigationDelegateProtocols = mutableListOf<DNavigationDelegateProtocol>()
  fun addNavigationDelegate(protocol: DNavigationDelegateProtocol) {
    navigationDelegateProtocols.add(protocol)
  }

  fun removeNavigationDelegate(protocol: DNavigationDelegateProtocol) {
    navigationDelegateProtocols.remove(protocol)
  }
}