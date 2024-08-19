package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.browser.callback.CreatePopupCallback
import com.teamdev.jxbrowser.browser.callback.OpenPopupCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.UrlLoadingPolicy
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.Signal

fun setupCreateWindowSignals(engine: DWebViewEngine) =
  CreateWindowSignals(Signal()).also { (createWindowSignal) ->
    engine.browser.set(CreatePopupCallback::class.java, CreatePopupCallback { event ->
      for (hook in engine.decidePolicyForCreateWindowHooks) {
        when (hook(event.targetUrl())) {
          UrlLoadingPolicy.Allow -> continue
          UrlLoadingPolicy.Block -> return@CreatePopupCallback CreatePopupCallback.Response.suppress()
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
  val createWindowSignal: Signal<IDWebView>,
)
