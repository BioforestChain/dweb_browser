package org.dweb_browser.helper.platform

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.dweb_browser.helper.globalDefaultScope

expect open class EnvSwitchCore() : EnvSwitchWatcher {
  fun isEnabled(switch: String): Boolean
  fun get(switch: String): String
  fun set(switch: String, value: String = "true")
  fun remove(switch: String)
}

open class EnvSwitchWatcher {
  private val changedFlow = MutableSharedFlow<String>()
  protected fun emitChanged(switch: String) {
    globalDefaultScope.launch(start = CoroutineStart.UNDISPATCHED) { changedFlow.emit(switch) }
  }

  fun watch(
    switch: String,
    initEvaluation: Boolean = true,
    block: suspend () -> Unit,
  ) {
    var first = initEvaluation
    globalDefaultScope.launch(start = CoroutineStart.UNDISPATCHED) {
      launch {
        if (first) {
          block()
        }
      }
      changedFlow.collect {
        if (it == switch) {
          first = false
          block()
        }
      }
    }
  }
}