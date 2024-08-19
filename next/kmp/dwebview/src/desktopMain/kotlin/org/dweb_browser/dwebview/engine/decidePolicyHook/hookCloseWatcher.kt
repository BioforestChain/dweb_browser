package org.dweb_browser.dwebview.engine.decidePolicyHook

import org.dweb_browser.dwebview.UrlLoadingPolicy
import org.dweb_browser.dwebview.engine.DWebViewEngine

fun DWebViewEngine.hookCloseWatcher() {
  decidePolicyForCreateWindowHooks.add { consumeToken ->
    closeWatcher.run {
      if (consuming.remove(consumeToken)) {
        resolveToken(consumeToken, applyWatcher(true))
        UrlLoadingPolicy.Block
      } else {
        UrlLoadingPolicy.Allow
      }
    }
  }
}