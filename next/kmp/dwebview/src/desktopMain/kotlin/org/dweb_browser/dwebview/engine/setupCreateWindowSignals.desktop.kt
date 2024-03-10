package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.browser.Browser
import com.teamdev.jxbrowser.browser.callback.OpenPopupCallback
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.Signal

fun setupCreateWindowSignals(engine: DWebViewEngine) =
  CreateWindowSignals(Signal(), Signal()).also { (beforeCreateWindowSignal, createWindowSignal) ->
    engine.browser.set(OpenPopupCallback::class.java, OpenPopupCallback { event ->
      event.scaleFactor()
      val browser = event.popupBrowser()
      engine.ioScope.launch {
        if (beforeCreateWindowSignal.isNotEmpty()) {
          val beforeCreateWindowEvent = BeforeCreateWindow(browser)
          beforeCreateWindowSignal.emit(beforeCreateWindowEvent)
          if (beforeCreateWindowEvent.isConsumed) {
            return@launch
          }
        }
        val openUrl = browser.url()
        val dwebView = IDWebView.create(
          DWebViewEngine(engine.remoteMM, DWebViewOptions(url = openUrl), browser), openUrl
        )
        createWindowSignal.emit(dwebView)
      }

      OpenPopupCallback.Response.proceed()
    })
  }

data class CreateWindowSignals(
  val beforeCreateWindowSignal: Signal<BeforeCreateWindow>,
  val createWindowSignal: Signal<IDWebView>,
)

class BeforeCreateWindow(val browser: Browser) {
  val url get() = browser.url()
  val isUserGesture = true
  var isConsumed = false
    private set

  fun consume() {
    isConsumed = true
  }
}