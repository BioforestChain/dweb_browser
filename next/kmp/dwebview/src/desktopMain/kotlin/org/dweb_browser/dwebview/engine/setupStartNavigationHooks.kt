package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.navigation.callback.StartNavigationCallback
import org.dweb_browser.dwebview.UrlLoadingPolicy

fun setupStartNavigationHooks(engine: DWebViewEngine) =
  mutableListOf<(url: String) -> UrlLoadingPolicy>().also { hooks ->
    engine.browser.navigation()
      .set(StartNavigationCallback::class.java, StartNavigationCallback { params ->
        val url = params.url()
        for (hook in hooks) {
          return@StartNavigationCallback when (hook(url)) {
            UrlLoadingPolicy.Allow -> continue
            UrlLoadingPolicy.Block -> StartNavigationCallback.Response.ignore()
          }
        }
        StartNavigationCallback.Response.start()
      })
  }