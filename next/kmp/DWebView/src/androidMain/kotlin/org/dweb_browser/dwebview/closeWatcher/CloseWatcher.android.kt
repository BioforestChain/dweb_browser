package org.dweb_browser.dwebview.closeWatcher

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.*

@SuppressLint("JavascriptInterface")
class CloseWatcher(override val engine: DWebViewEngine) : DCloseWatcher(engine) {

  init {
    engine.addJavascriptInterface(
      object {
        /**
         * js 创建 CloseWatcher
         */
        @JavascriptInterface
        fun registryToken(consumeToken: String) {
          if (consumeToken.isNullOrBlank()) {
            throw Exception("CloseWatcher.registryToken invalid arguments");
          }
          consuming.add(consumeToken)
          engine.mainScope.launch {
            engine.evaluateJavascript("open('$consumeToken')", {})
          }
        }

        /**
         * js主动关闭 CloseWatcher
         */
        @JavascriptInterface
        fun tryClose(id: String) =
          watchers.find { watcher -> watcher.id == id }?.also {
            engine.mainScope.launch { close(it) }
          }
      },
      JS_POLYFILL_KIT
    )
  }
}