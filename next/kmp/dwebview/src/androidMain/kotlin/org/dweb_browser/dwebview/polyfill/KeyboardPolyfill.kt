package org.dweb_browser.dwebview.polyfill

import org.dweb_browser.dwebview.engine.DWebViewEngine

fun setupKeyboardPolyfill(engine: DWebViewEngine) {
  engine.addDocumentStartJavaScript(DwebViewAndroidPolyfill.KeyBoard)
}