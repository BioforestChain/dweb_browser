package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.browser.Browser
import com.teamdev.jxbrowser.browser.CloseOptions
import com.teamdev.jxbrowser.js.JsException
import com.teamdev.jxbrowser.js.JsPromise
import com.teamdev.jxbrowser.view.swing.BrowserView
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.platform.desktop.webview.WebviewEngine
import java.util.function.Consumer

class DWebViewEngine(val options: DWebViewOptions) {
  val browser: Browser = WebviewEngine.hardwareAccelerated.newBrowser()
  val wrapperView: BrowserView by lazy { BrowserView.newInstance(browser) }
  val mainFrame get() = browser.mainFrame().get()


  /**
   * 执行异步JS代码，需要传入一个表达式
   */
  suspend fun evaluateAsyncJavascriptCode(
    script: String, afterEval: (suspend () -> Unit)? = null
  ): String {
    val deferred = CompletableDeferred<String>()

    runCatching {
      mainFrame.executeJavaScript(
        "(async()=>{return ($script)})().then(r=>JSON.stringify(r),e=>String(e))",
        Consumer<JsPromise> { promise ->
          promise
            .then {
              deferred.complete(it[0] as String)
            }
            .catchError {
              deferred.completeExceptionally(JsException(it[0] as String))
            }
        })
    }.getOrElse { deferred.completeExceptionally(it) }
    afterEval?.invoke()

    return deferred.await()
  }

  fun destroy() {
    browser.close(CloseOptions.newBuilder().build())
  }
}