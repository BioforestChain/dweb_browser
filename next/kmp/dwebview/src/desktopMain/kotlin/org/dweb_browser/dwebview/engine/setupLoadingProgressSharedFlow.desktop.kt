package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.navigation.event.LoadFinished
import com.teamdev.jxbrowser.navigation.event.LoadProgressChanged
import com.teamdev.jxbrowser.navigation.event.LoadStarted
import kotlinx.coroutines.flow.MutableStateFlow

fun setupLoadingProgressStateFlow(engine: DWebViewEngine) =
  MutableStateFlow(1f).also { stateFlow ->
    fun tryEmit(value: Float) {
      if (value != stateFlow.replayCache.lastOrNull()) {
        stateFlow.tryEmit(value)
      }
    }
    engine.browser.navigation().apply {
      on(LoadStarted::class.java) {
        tryEmit(0f)
      }
      on(LoadProgressChanged::class.java) {
        tryEmit(it.progress().toFloat())
      }
      on(LoadFinished::class.java) {
        tryEmit(1f)
      }
    }
  }