package org.dweb_browser.dwebview.engine

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.ScrollChangeEvent
import org.dweb_browser.helper.Signal
import java.util.function.Consumer

fun setupScrollSignal(engine: DWebViewEngine) = Signal<ScrollChangeEvent>().also { signal ->
  engine.injectJsAction {
    val jsEventHandler = atomic<JsJEventHandler?>(null)
    signal.whenNoEmpty {
      val handler: JsJEventHandler = {
        executeJavaScript("((document.scrollingElement?.scrollLeft||0)*devicePixelRatio)+','+((document.scrollingElement?.scrollTop||0)*devicePixelRatio)",
          Consumer<String> { result ->
            val (scrollX, scrollY) = result.split(',').map { it.toInt() }
            engine.lifecycleScope.launch {
              signal.emit(ScrollChangeEvent(scrollX, scrollY))
            }
          })
      }
      jsEventHandler.update {
        window().addEventListener("scroll", handler)
        handler
      }
    }

    signal.whenEmpty {
      jsEventHandler.update { handler ->
        if (handler != null) {
          window().removeEventListener("scroll", handler)
        }
        null
      }
    }
  }
}

