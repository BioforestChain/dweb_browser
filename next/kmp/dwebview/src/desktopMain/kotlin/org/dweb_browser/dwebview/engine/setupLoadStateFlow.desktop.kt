package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.navigation.event.FrameDocumentLoadFinished
import com.teamdev.jxbrowser.navigation.event.FrameLoadFailed
import com.teamdev.jxbrowser.navigation.event.FrameLoadFinished
import com.teamdev.jxbrowser.navigation.event.LoadFinished
import com.teamdev.jxbrowser.navigation.event.LoadProgressChanged
import com.teamdev.jxbrowser.navigation.event.LoadStarted
import com.teamdev.jxbrowser.navigation.event.NavigationEvent
import com.teamdev.jxbrowser.navigation.event.NavigationFinished
import com.teamdev.jxbrowser.navigation.event.NavigationRedirected
import com.teamdev.jxbrowser.navigation.event.NavigationStarted
import com.teamdev.jxbrowser.navigation.event.NavigationStopped
import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.dwebview.WebLoadErrorState
import org.dweb_browser.dwebview.WebLoadStartState
import org.dweb_browser.dwebview.WebLoadState
import org.dweb_browser.dwebview.WebLoadSuccessState
import org.dweb_browser.dwebview.debugDWebView

fun setupLoadStateFlow(engine: DWebViewEngine, initUrl: String) = MutableStateFlow(
  when (initUrl) {
    "", "about:blank" -> WebLoadSuccessState("about:blank")
    else -> WebLoadStartState(initUrl)
  }
).also {
  setupLoadStateFlow(engine, it)
}

private fun setupLoadStateFlow(
  engine: DWebViewEngine,
  flow: MutableStateFlow<WebLoadState>,
) {
  fun emitSignal(state: WebLoadState) {
    flow.value = state
  }
  engine.browser.navigation().apply {
    if (debugDWebView.isEnable) {

      on(FrameDocumentLoadFinished::class.java) {
        debugDWebView("NavEvent", "FrameDocumentLoadFinished")
      }
      on(FrameLoadFailed::class.java) {
        debugDWebView("NavEvent", "FrameLoadFailed url=${it.url()} error=${it.error()}")
      }
      on(FrameLoadFinished::class.java) {
        debugDWebView("NavEvent", "FrameLoadFinished url=${it.url()}")
      }
      on(LoadFinished::class.java) {
        debugDWebView("NavEvent", "LoadFinished")
      }
      on(LoadProgressChanged::class.java) {
        debugDWebView("NavEvent", "LoadProgressChanged progress=${it.progress()}")
      }
      on(LoadStarted::class.java) {
        debugDWebView("NavEvent", "LoadStarted")
      }
      on(NavigationEvent::class.java) {
        debugDWebView("NavEvent", "NavigationEvent")
      }
      on(NavigationFinished::class.java) {
        debugDWebView("NavEvent", "NavigationFinished url=${it.url()}")
      }
      on(NavigationRedirected::class.java) {
        debugDWebView("NavEvent", "NavigationRedirected destinationUrl=${it.destinationUrl()}")
      }
      on(NavigationStarted::class.java) {
        debugDWebView("NavEvent", "NavigationStarted url=${it.url()}")
      }
      on(NavigationStopped::class.java) {
        debugDWebView("NavEvent", "NavigationStopped")
      }
    }
    // NavigationStarted 意味着 domain 开始解析
    on(NavigationStarted::class.java) {
      if (it.isInMainFrame) {
        emitSignal(WebLoadStartState(it.url()))
      }
    }
    // NavigationStarted 意味着 domain 解析完成，这时候的url是稳定的
    on(NavigationFinished::class.java) {
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
    on(LoadFinished::class.java) {
      emitSignal(WebLoadSuccessState(browser().url()))
    }
  }
}