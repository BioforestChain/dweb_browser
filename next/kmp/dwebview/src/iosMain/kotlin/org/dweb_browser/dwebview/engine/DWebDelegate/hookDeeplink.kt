package org.dweb_browser.dwebview.engine.DWebDelegate

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.dwebview.UrlLoadingPolicy
import org.dweb_browser.dwebview.base.isWebUrlScheme
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.dwebview.engine.DWebNavigationDelegate
import org.dweb_browser.dwebview.engine.DWebUIDelegate
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.dwebview.engine.dwebHelper
import platform.Foundation.NSURLRequest

internal fun DWebNavigationDelegate.hookDeeplink() {
  decidePolicyForNavigationActionHooks.add {
    when (engine.hookDeeplink(decidePolicyForNavigationAction.request)) {
      true -> UrlLoadingPolicy.Block
      else -> UrlLoadingPolicy.Allow
    }
  }
}


internal fun DWebUIDelegate.hookDeeplink() {
  createWebViewHooks.add {
    return@add when (engine.hookDeeplink(forNavigationAction.request)) {
      true -> DWebUIDelegate.CreateWebViewHookPolicyDeny
      else -> DWebUIDelegate.CreateWebViewHookPolicyContinue
    }
//
//    val url = navigationAction.request.URL?.absoluteString ?: return@add null
//
//    val urlScheme = url.split(':', limit = 2).first()
//    engine.lifecycleScope.launch {
//      if (urlScheme == "dweb") {
//        engine.remoteMM.nativeFetch(url)
//      } else if (isWebUrlScheme(urlScheme)) {
//        var target = "_self"
//        if (navigationAction.targetFrame == null) {
//          target = "_blank"
//        }
//
//        engine.remoteMM.nativeFetch(buildUrlString("dweb://openinbrowser") {
//          parameters["url"] = url
//          parameters["target"] = target
//        })
//      }
//    }
//    null
  }
}

@OptIn(ExperimentalForeignApi::class)
private fun DWebViewEngine.hookDeeplink(request: NSURLRequest): Boolean {
  val url = request.URL
  val scheme = url?.scheme ?: "http"
  if (url != null && !isWebUrlScheme(scheme)) {
    if (scheme == "dweb") {
      lifecycleScope.launch {
        remoteMM.nativeFetch(url.absoluteString!!)
      }
      return true
    }

    // 无法使用 uiApp.canOpenURL，因为没有在 info.plist 注册特殊scheme，会导致无法打开scheme链接
    // 无法使用 uiApp.openURL，因为当前kotlin对接的iOS openURL已经失效，需要自己对接 open 方法
    // val uiApp = remoteMM.getUIApplication()
    dwebHelper.openURL(url) { res ->
      debugDWebView("hookDeeplink openURL -> ", res)
    }
    return true
  }
  return false
}