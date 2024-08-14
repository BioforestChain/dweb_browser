package org.dweb_browser.dwebview.engine.DWebDelegate

import org.dweb_browser.dwebview.UrlLoadingPolicy
import org.dweb_browser.dwebview.WebBeforeUnloadArgs
import org.dweb_browser.dwebview.WebLoadSuccessState
import org.dweb_browser.dwebview.engine.DWebNavigationDelegate

internal fun DWebNavigationDelegate.hookBeforeUnload() {
  decidePolicyForNavigationActionHooks.add { _, decidePolicyForNavigationAction ->
    /// navigationAction.navigationType : https://developer.apple.com/documentation/webkit/wknavigationtype/
    when {
      engine.beforeUnloadSignal.isNotEmpty() -> {
        val message = when (decidePolicyForNavigationAction.navigationType) {
          // reload
          3L -> "重新加载此网站？"
          else -> "离开此网站？"
        }
        val args = WebBeforeUnloadArgs(message)
        engine.beforeUnloadSignal.emit(args)
        when {
          args.waitHookResults() -> UrlLoadingPolicy.Allow
          else -> {
            engine.loadStateChangeSignal.emit(
              WebLoadSuccessState(engine.evalAsyncJavascript("location.href").await())
            )
            UrlLoadingPolicy.Block
          }
        }
      }

      else -> UrlLoadingPolicy.Allow
    }
  }
}
