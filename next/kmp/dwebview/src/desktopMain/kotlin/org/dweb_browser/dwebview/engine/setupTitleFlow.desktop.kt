package org.dweb_browser.dwebview.engine


import com.teamdev.jxbrowser.browser.event.TitleChanged
import kotlinx.coroutines.flow.MutableStateFlow

fun setupTitleFlow(engine: DWebViewEngine) = MutableStateFlow("").also { stateFlow ->
  engine.browser.on(TitleChanged::class.java) {
    stateFlow.value = it.title()
  }
}