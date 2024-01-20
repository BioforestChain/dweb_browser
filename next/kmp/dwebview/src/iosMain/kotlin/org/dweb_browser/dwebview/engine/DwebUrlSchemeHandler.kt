package org.dweb_browser.dwebview.engine


import org.dweb_browser.core.module.MicroModule
import platform.WebKit.WKURLSchemeHandlerProtocol
import platform.WebKit.WKURLSchemeTaskProtocol
import platform.WebKit.WKWebView
import platform.darwin.NSObject

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE", "CONFLICTING_OVERLOADS")
class DwebURLSchemeHandler(microModule: MicroModule) : NSObject(),
  WKURLSchemeHandlerProtocol {
  val helper = DURLSchemeHandlerHelper(microModule)

  @Suppress("CONFLICTING_OVERLOADS")
  override fun webView(webView: WKWebView, startURLSchemeTask: WKURLSchemeTaskProtocol) {
    val url = startURLSchemeTask.request.URL?.absoluteString ?: return run {
      startURLSchemeTask.didFinish()
    }
    helper.startURLSchemeTask(webView, startURLSchemeTask, url)
  }

  override fun webView(webView: WKWebView, stopURLSchemeTask: WKURLSchemeTaskProtocol) {
    helper.stopURLSchemeTask(webView, stopURLSchemeTask)
  }
}