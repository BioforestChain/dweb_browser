package org.dweb_browser.dwebview.closeWatcher

import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.withMainContext

internal class CloseWatcher(val engine: DWebViewEngine) : DCloseWatcher(engine) {
  suspend fun registryToken(consumeToken: String) {
    consuming.add(consumeToken)
    withMainContext {
      engine.evaluateJavascriptSync("open('$consumeToken')")
    }
  }

  fun tryClose(id: String) {
    watchers.find { watcher -> watcher.id == id }?.also {
      engine.mainScope.launch { close(it) }
    }
  }
}