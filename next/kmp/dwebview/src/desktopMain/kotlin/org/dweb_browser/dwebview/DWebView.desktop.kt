package org.dweb_browser.dwebview

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.ioAsyncExceptionHandler

actual suspend fun IDWebView.Companion.create(
  mm: MicroModule,
  options: DWebViewOptions
): IDWebView {
  TODO("Not yet implemented")
}

abstract class DWebView(
  val viewEngine: DWebViewEngine,
  initUrl: String? = null
) : IDWebView(initUrl ?: viewEngine.options.url) {
  companion object {
    val prepare = SuspendOnce {
      System.setProperty(
        "jxbrowser.license.key",
        //
        "1BNDIEOFAZ1Z8R8VNNG4W07HLC9173JJW3RT0P2G9Y28L9YFFIWDBRFNFLFDQBKXAHO9ZE" // Only For JxBrowser 7.26
      );
    }

    init {
      CoroutineScope(ioAsyncExceptionHandler).launch {
        prepare()
      }
    }
  }
}