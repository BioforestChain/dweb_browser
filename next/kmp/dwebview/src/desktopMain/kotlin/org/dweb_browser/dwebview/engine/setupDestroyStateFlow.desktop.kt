package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.browser.event.BrowserClosed
import org.dweb_browser.dwebview.DestroyStateSignal

fun setupDestroyStateSignal(engine: DWebViewEngine) =
  DestroyStateSignal(engine.lifecycleScope).also { stateSignal ->
    engine.browser.on(BrowserClosed::class.java) {
      stateSignal.doDestroy()
    }
  }
