package org.dweb_browser.dwebview.proxy

import android.annotation.SuppressLint
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.withMainContext

object DwebViewProxyOverride {
  @SuppressLint("RequiresFeature")
  val prepare = SuspendOnce {
    DwebViewProxy.prepare();
    val canLoadPage = CompletableDeferred<Unit>()
    withMainContext {
      val canProxyOverride = WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)
      if (canProxyOverride) {
        val address = DwebViewProxy.ProxyUrl
        val proxyConfig = ProxyConfig.Builder().addProxyRule(address)
          .addDirect()
          .build()
        ProxyController.getInstance().setProxyOverride(proxyConfig, {
          debugDWebView("reverse_proxy listener", address)
          canLoadPage.complete(Unit)
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