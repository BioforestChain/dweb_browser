package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.navigation.callback.StartNavigationCallback
import org.dweb_browser.dwebview.OverrideUrlLoadingParams
import org.dweb_browser.dwebview.UrlLoadingPolicy

fun setupOverrideUrlLoadingHooks(engine: DWebViewEngine) =
  mutableListOf<OverrideUrlLoadingParams.() -> UrlLoadingPolicy>().also { hooks ->
    engine.browser.navigation()
      .set(StartNavigationCallback::class.java, StartNavigationCallback { navParams ->
        val params = OverrideUrlLoadingParams(navParams.url(), navParams.isMainFrame)
        for (hook in hooks) {
          return@StartNavigationCallback when (params.hook()) {
            UrlLoadingPolicy.Allow -> continue
            UrlLoadingPolicy.Block -> StartNavigationCallback.Response.ignore()
          }
        }
        StartNavigationCallback.Response.start()
      })
  }