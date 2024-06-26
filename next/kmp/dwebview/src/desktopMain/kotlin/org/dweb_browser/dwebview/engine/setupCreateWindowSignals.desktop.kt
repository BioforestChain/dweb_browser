package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.browser.callback.CreatePopupCallback
import com.teamdev.jxbrowser.browser.callback.OpenPopupCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.Signal

fun setupCreateWindowSignals(engine: DWebViewEngine) =
  CreateWindowSignals(Signal(), Signal()).also { (beforeCreateWindowSignal, createWindowSignal) ->
    engine.browser.set(CreatePopupCallback::class.java, CreatePopupCallback { event ->
      // 如果是dweb deeplink链接，直接发起nativeFetch并返回suppress，否则OpenPopupCallback中的browser无法获取到url会导致无限循环
      if (event.targetUrl().startsWith("dweb://")) {
        engine.lifecycleScope.launch {
          engine.remoteMM.nativeFetch(event.targetUrl())
        }
        return@CreatePopupCallback CreatePopupCallback.Response.suppress()
      }

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
      engine.lifecycleScope.launch {
        val newEngine = DWebViewEngine(engine.remoteMM, DWebViewOptions(), browser)
        while (newEngine.getOriginalUrl().isEmpty()) {
          delay(5)
        }
        val dwebView = IDWebView.create(newEngine, newEngine.getOriginalUrl())
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