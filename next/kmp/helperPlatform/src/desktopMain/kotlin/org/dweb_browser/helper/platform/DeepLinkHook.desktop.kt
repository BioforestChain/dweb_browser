package org.dweb_browser.helper.platform

import kotlinx.coroutines.launch
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.globalDefaultScope

class DeepLinkHook private constructor() {
  companion object {
    val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
      DeepLinkHook()
    }
  }

  private val scope = globalDefaultScope

  private val deeplinkSignal = Signal<String>()

  val onLink = deeplinkSignal.toListener()

  fun emitLink(url: String) {
    scope.launch {
      deeplinkSignal.emit(url)
    }
  }
}
