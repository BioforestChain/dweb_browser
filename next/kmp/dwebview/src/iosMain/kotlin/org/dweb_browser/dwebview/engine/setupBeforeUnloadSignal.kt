package org.dweb_browser.dwebview.engine

import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.dwebview.UrlLoadingPolicy
import org.dweb_browser.dwebview.WebBeforeUnloadArgs
import org.dweb_browser.dwebview.WebLoadState
import org.dweb_browser.dwebview.WebLoadSuccessState
import org.dweb_browser.helper.Signal

internal fun setupBeforeUnloadSignal(
  engine: DWebViewEngine,
  dwebNavigationDelegate: DWebNavigationDelegate,
  loadStateFlow: MutableStateFlow<WebLoadState>,
) = Signal<WebBeforeUnloadArgs>().also { beforeUnloadSignal ->
  dwebNavigationDelegate.decidePolicyForNavigationActionHooks.add {
    /// navigationAction.navigationType : https://developer.apple.com/documentation/webkit/wknavigationtype/
    when {
      beforeUnloadSignal.isNotEmpty() -> {
        val message = when (decidePolicyForNavigationAction.navigationType) {
          // reload
          3L -> "重新加载此网站？"
          else -> "离开此网站？"
        }
        val args = WebBeforeUnloadArgs(message)
        beforeUnloadSignal.emit(args)
        when {
          args.waitHookResults() -> UrlLoadingPolicy.Allow
          else -> {
            loadStateFlow.emit(
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