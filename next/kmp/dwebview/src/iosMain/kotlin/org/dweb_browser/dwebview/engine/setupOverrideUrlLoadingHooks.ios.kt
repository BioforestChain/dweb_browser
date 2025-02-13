package org.dweb_browser.dwebview.engine

import org.dweb_browser.dwebview.OverrideUrlLoadingParams
import org.dweb_browser.dwebview.UrlLoadingPolicy

fun setupOverrideUrlLoadingHooks(
  engine: DWebViewEngine,
  dwebNavigationDelegate: DWebNavigationDelegate,
) = mutableListOf<OverrideUrlLoadingParams.() -> UrlLoadingPolicy>().also { hooks ->
  dwebNavigationDelegate.decidePolicyForNavigationActionHooks.add {
    val url = decidePolicyForNavigationAction.request.URL.toString()
    @Suppress("UNNECESSARY_SAFE_CALL") val params = OverrideUrlLoadingParams(
      url,
      decidePolicyForNavigationAction.targetFrame?.isMainFrame()
      // 这里的 sourceFrame 是可能为 null 的，所以 必须 ?.
        ?: decidePolicyForNavigationAction.sourceFrame?.isMainFrame() ?: true
    )
    for (hook in hooks) {
      return@add when (params.hook()) {
        UrlLoadingPolicy.Allow -> continue
        UrlLoadingPolicy.Block -> UrlLoadingPolicy.Block
      }
    }
    UrlLoadingPolicy.Allow
  }
}