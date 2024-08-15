package org.dweb_browser.dwebview.engine

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.dwebview.polyfill.DWebViewFaviconMessageHandler
import org.dweb_browser.dwebview.polyfill.DwebViewIosPolyfill
import org.dweb_browser.dwebview.polyfill.FaviconPolyfill
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.platform.toImageBitmap
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.defaultHttpPureClient
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime

fun setupIconFlow(engine: DWebViewEngine) = MutableStateFlow("").also { stateFlow ->
  engine.configuration.userContentController.apply {
    ifNoDefineUserScript(DwebViewIosPolyfill.Favicon) {
      addScriptMessageHandler(
        scriptMessageHandler = DWebViewFaviconMessageHandler { iconHref ->
          stateFlow.value = iconHref
          engine.setIcon(iconHref)
        },
        contentWorld = FaviconPolyfill.faviconContentWorld,
        name = "favicons"
      )
      addUserScript(
        WKUserScript(
          source = DwebViewIosPolyfill.Favicon,
          injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
          forMainFrameOnly = true,
          inContentWorld = FaviconPolyfill.faviconContentWorld,
        ),
      )
    }
  }
}

fun setupIconBitmapFlow(engine: DWebViewEngine) =
  MutableStateFlow<ImageBitmap?>(null).also { stateFlow ->
    engine.iconFlow.collectIn(engine.lifecycleScope) { faviconHref ->
      stateFlow.value = if (faviconHref.isNotEmpty()) {
        runCatching {
          defaultHttpPureClient.fetch(PureClientRequest(faviconHref, method = PureMethod.GET))
            .binary().toImageBitmap()
        }.getOrNull()
      } else null
    }
  }

