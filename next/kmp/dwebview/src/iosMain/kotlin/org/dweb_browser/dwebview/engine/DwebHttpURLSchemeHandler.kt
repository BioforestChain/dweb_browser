package org.dweb_browser.dwebview.engine

import kotlinx.cinterop.ObjCSignatureOverride
import org.dweb_browser.core.module.MicroModule
import platform.WebKit.WKURLSchemeHandlerProtocol
import platform.WebKit.WKURLSchemeTaskProtocol
import platform.WebKit.WKWebView
import platform.darwin.NSObject

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE", "CONFLICTING_OVERLOADS")
class DwebHttpURLSchemeHandler(microModule: MicroModule.Runtime) : NSObject(), WKURLSchemeHandlerProtocol {
  val helper = DURLSchemeHandlerHelper(microModule)

  @ObjCSignatureOverride
  override fun webView(webView: WKWebView, startURLSchemeTask: WKURLSchemeTaskProtocol) {
    val url = startURLSchemeTask.request.URL?.absoluteString ?: return run {
      startURLSchemeTask.didFinish()
    }

    /**
     * 剔除 dweb+http: dweb+https: 前面这部分
     */
    val pureUrl = url.replace("dweb+", "")
    helper.startURLSchemeTask(webView, startURLSchemeTask, pureUrl)
  }

  @ObjCSignatureOverride
  override fun webView(webView: WKWebView, stopURLSchemeTask: WKURLSchemeTaskProtocol) {
    helper.stopURLSchemeTask(webView, stopURLSchemeTask)
  }
}