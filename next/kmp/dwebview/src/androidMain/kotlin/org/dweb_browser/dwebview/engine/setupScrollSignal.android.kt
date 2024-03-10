package org.dweb_browser.dwebview.engine

import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.ScrollChangeEvent
import org.dweb_browser.helper.Signal

fun setupScrollSignal(engine: DWebViewEngine) = Signal<ScrollChangeEvent>().also { signal ->
  signal.whenNoEmpty {
    engine.setOnScrollChangeListener { _, scrollX, scrollY, _, _ ->
      engine.ioScope.launch {
        signal.emit(ScrollChangeEvent(scrollX, scrollY))
      }
    }
  }
  signal.whenEmpty {
    engine.setOnScrollChangeListener(null)
  }
}