package org.dweb_browser.dwebview.proxy

import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.withMainContext

object DwebViewProxyOverride {
  val prepare = SuspendOnce {
    DwebViewProxy.prepare();
    val canLoadPage = CompletableDeferred<Unit>()
    withMainContext {
      val canProxyOverride = WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)
      if (canProxyOverride) {
        val address = DwebViewProxy.ProxyUrl
        debugDWebView("reverse_proxy proxyAddress", address)
        val proxyConfig = ProxyConfig.Builder().addProxyRule(address)
          .addDirect()
          .build()
        ProxyController.getInstance().setProxyOverride(proxyConfig, {
          GlobalScope.launch{
            debugDWebView("reverse_proxy listener", "okk")
            delay(50)
            canLoadPage.complete(Unit)
          }
        }, {
          debugDWebView("reverse_proxy listener", "effect")
        })
      } else {
        canLoadPage.complete(Unit)
      }
    }
    canLoadPage.await()
  }
}