package org.dweb_browser.dwebview.polyfill

import com.teamdev.jxbrowser.js.JsAccessible

import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.Signal

class FaviconPolyfill(val engine: DWebViewEngine) {
  companion object {
    const val JS_POLYFILL_KIT = "__native_favicon_kit__"

  }

  var href: String = ""
    private set;
  private val changeSignal = Signal<String>()
  val onChange = changeSignal.toListener()

  init {

    engine.addDocumentStartJavaScript(DwebViewDesktopPolyfill.Favicon)
    engine.addJavascriptInterface(object {
      @JsAccessible
      fun emitChange(faviconHref: String) {
        href = faviconHref
        engine.ioScope.launch {
          changeSignal.emit(faviconHref)
        }
      }
    }, JS_POLYFILL_KIT)
  }
}