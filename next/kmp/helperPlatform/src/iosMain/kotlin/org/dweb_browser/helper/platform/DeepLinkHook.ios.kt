package org.dweb_browser.helper.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.ioAsyncExceptionHandler

class DeepLinkHook private constructor() {
  companion object {
    val deepLinkHook by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
      DeepLinkHook()
    }
  }

  private val scope = CoroutineScope(ioAsyncExceptionHandler)

  val deeplinkSignal = Signal<String>();
  fun emitOnInit(url: String) {
    scope.launch {
      deeplinkSignal.emit(url)
    }
  }
}
