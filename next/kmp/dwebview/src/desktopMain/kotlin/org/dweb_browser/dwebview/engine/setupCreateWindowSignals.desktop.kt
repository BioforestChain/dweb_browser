package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.browser.callback.CreatePopupCallback
import com.teamdev.jxbrowser.browser.callback.OpenPopupCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.Signal

fun setupCreateWindowSignals(engine: DWebViewEngine) =
  CreateWindowSignals(Signal(), Signal()).also { (beforeCreateWindowSignal, createWindowSignal) ->
    engine.browser.set(CreatePopupCallback::class.java, CreatePopupCallback { event ->
      if (beforeCreateWindowSignal.isNotEmpty()) {
        val beforeCreateWindowEvent = BeforeCreateWindow(event.targetUrl())
        runBlocking {
          beforeCreateWindowSignal.emit(beforeCreateWindowEvent)
        }
        if (beforeCreateWindowEvent.isConsumed) {
          return@CreatePopupCallback CreatePopupCallback.Response.suppress()
        }
      }
      CreatePopupCallback.Response.create()
    })
    engine.browser.set(OpenPopupCallback::class.java, OpenPopupCallback { event ->
      val browser = event.popupBrowser()
      engine.ioScope.launch {
        var openUrl = ""
        while (openUrl.isEmpty()) {
          openUrl = browser.url()
          delay(5)
        }
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

class BeforeCreateWindow(val url: String) {
  val isUserGesture = true
  var isConsumed = false
    private set

  fun consume() {
    isConsumed = true
  }
}