package org.dweb_browser.dwebview.engine.decidePolicyHook

import kotlinx.coroutines.launch
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.dwebview.UrlLoadingPolicy
import org.dweb_browser.dwebview.engine.DWebViewEngine

internal fun DWebViewEngine.hookDeeplink() {
  decidePolicyForCreateWindowHooks.add { url ->
    if (url.startsWith("dweb://")) {
      lifecycleScope.launch {
        remoteMM.nativeFetch(url)
      }

      UrlLoadingPolicy.Block
    } else {
      UrlLoadingPolicy.Allow
    }
  }
  overrideUrlLoadingHooks.add {
    if (url.startsWith("dweb://")) {
      lifecycleScope.launch {
        remoteMM.nativeFetch(url)
      }

      UrlLoadingPolicy.Block
    } else {
      UrlLoadingPolicy.Allow
    }
  }
}