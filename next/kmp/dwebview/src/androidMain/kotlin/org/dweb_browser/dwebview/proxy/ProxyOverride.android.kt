package org.dweb_browser.dwebview.proxy

import android.annotation.SuppressLint
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.filterNotNull
import org.dweb_browser.core.http.dwebProxyService
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.withMainContext

object DWebViewProxyOverride {
  @SuppressLint("RequiresFeature")
  val prepare = SuspendOnce {
    val canLoadPage = CompletableDeferred<Unit>()
    dwebProxyService.start()
    dwebProxyService.proxyUrl.filterNotNull().collectIn { proxyUrl ->
      withMainContext {
        val canProxyOverride = WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)
        if (canProxyOverride) {
          val proxyConfig = ProxyConfig.Builder().addProxyRule(proxyUrl)
            .addDirect()
            .build()
          ProxyController.getInstance().setProxyOverride(proxyConfig, {
            debugDWebView("reverse_proxy listener", proxyUrl)
            canLoadPage.complete(Unit)
          }, {
            debugDWebView("reverse_proxy listener", "effect")
          })
        } else {
          canLoadPage.complete(Unit)
        }
      }
    }

    canLoadPage.await()
  }
}