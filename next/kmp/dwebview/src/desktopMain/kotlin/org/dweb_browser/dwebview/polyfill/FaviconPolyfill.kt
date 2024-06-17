package org.dweb_browser.dwebview.polyfill

import com.teamdev.jxbrowser.js.JsAccessible
import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.dwebview.engine.DWebViewEngine

class FaviconPolyfill(val engine: DWebViewEngine) {
  companion object {
    const val JS_POLYFILL_KIT = "__native_favicon_kit__"
  }

  val urlFlow = MutableStateFlow("")

  init {
    engine.addDocumentStartJavaScript(DwebViewDesktopPolyfill.Favicon)
    engine.addJavascriptInterface(object {
      @JsAccessible
      fun emitChange(faviconHref: String) {
        urlFlow.value = faviconHref
      }
    }, JS_POLYFILL_KIT)
  }
}