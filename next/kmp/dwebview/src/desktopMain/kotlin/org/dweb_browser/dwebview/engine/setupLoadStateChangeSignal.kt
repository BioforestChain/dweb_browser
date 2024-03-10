package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.navigation.event.FrameLoadFailed
import com.teamdev.jxbrowser.navigation.event.FrameLoadFinished
import com.teamdev.jxbrowser.navigation.event.NavigationStarted
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.WebLoadErrorState
import org.dweb_browser.dwebview.WebLoadStartState
import org.dweb_browser.dwebview.WebLoadState
import org.dweb_browser.dwebview.WebLoadSuccessState
import org.dweb_browser.helper.Signal

fun setupLoadStateChangeSignal(engine: DWebViewEngine) = Signal<WebLoadState>().also { signal ->
  fun emitSignal(state: WebLoadState) {
    engine.ioScope.launch {
      signal.emit(state)
    }
  }
  engine.browser.navigation().apply {
    on(NavigationStarted::class.java) {
      if (it.isInMainFrame) {
        emitSignal(WebLoadStartState(it.url()))
      }
    }
    on(FrameLoadFinished::class.java) {
      if (it.frame() == engine.mainFrame) {
        emitSignal(WebLoadSuccessState(it.url()))
      }
    }
    on(FrameLoadFailed::class.java) {
      if (it.frame() == engine.mainFrame) {
        emitSignal(WebLoadErrorState(it.url(), it.error().name))
      }
    }
  }
}