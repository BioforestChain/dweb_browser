package org.dweb_browser.dwebview.engine


import kotlinx.cinterop.ObjCSignatureOverride
import org.dweb_browser.core.module.MicroModule
import platform.WebKit.WKURLSchemeHandlerProtocol
import platform.WebKit.WKURLSchemeTaskProtocol
import platform.WebKit.WKWebView
import platform.darwin.NSObject

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE", "CONFLICTING_OVERLOADS")
class DwebURLSchemeHandler(microModule: MicroModule.Runtime) : NSObject(),
  WKURLSchemeHandlerProtocol {
  val helper = DURLSchemeHandlerHelper(microModule)

  @ObjCSignatureOverride
  override fun webView(webView: WKWebView, startURLSchemeTask: WKURLSchemeTaskProtocol) {
    val url = startURLSchemeTask.request.URL?.absoluteString ?: return run {
      startURLSchemeTask.didFinish()
    }
    helper.startURLSchemeTask(webView, startURLSchemeTask, url)
  }

  @ObjCSignatureOverride
  override fun webView(webView: WKWebView, stopURLSchemeTask: WKURLSchemeTaskProtocol) {
    helper.stopURLSchemeTask(webView, stopURLSchemeTask)
  }
}