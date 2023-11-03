package org.dweb_browser.dwebview

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.cancel
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.dwebview.engine.WebViewEvaluator
import platform.Foundation.NSArray
import platform.WebKit.WKContentWorld
import platform.WebKit.WKFrameInfo

class DWebView(
  private val engine: DWebViewEngine,
) : IDWebView {
  private suspend fun loadUrl(task: LoadUrlTask): String {
    engine.loadUrl(task.url)
    engine.addNavigationDelegate(DNavigationDelegateProtocol(decidePolicyForNavigationAction = { _, action, _ ->
      this();
      task.deferred.complete(
        action.request.URL?.absoluteString ?: "about:blank"
      )
    },
      didFailNavigation = { _, _, error ->
        this();
        task.deferred.completeExceptionally(Exception("[${error.code}] ${error.description}"))
      }
    ));
    task.deferred.invokeOnCompletion {
      engine.setNavigationDelegate(null)
    }
    return task.deferred.await()
  }

  private val loadUrlTask = atomic<LoadUrlTask?>(null)

  override suspend fun loadUrl(url: String, force: Boolean) = loadUrlTask.getAndUpdate { preTask ->
    if (!force && preTask?.url == url) {
      return@getAndUpdate preTask
    } else {
      preTask?.deferred?.cancel(CancellationException("load new url: $url"));
    }
    val newTask = LoadUrlTask(url)
    loadUrl(newTask)
    newTask.deferred.invokeOnCompletion {
      loadUrlTask.getAndUpdate { preTask ->
        if (preTask == newTask) null else preTask
      }
    }
    newTask
  }!!.deferred.await()

  override suspend fun getUrl(): String = loadUrlTask.value?.url ?: "about:blank"


  override suspend fun getTitle(): String {
    return engine.title ?: ""
  }

  override suspend fun getIcon(): String {
    TODO("Not yet implemented")
  }

  override suspend fun destroy() {
    loadUrl("about:blank", true)
    engine.mainScope.cancel(null)
    engine.removeFromSuperview()
  }

  override suspend fun canGoBack(): Boolean {
    return engine.canGoBack
  }

  override suspend fun canGoForward(): Boolean {
    return engine.canGoForward
  }

  override suspend fun goBack(): Boolean {
    return if (engine.canGoBack) {
      engine.goBack()
      true
    } else {
      false
    }
  }

  override suspend fun goForward(): Boolean {
    return if(engine.canGoForward) {
      engine.goForward()
      true
    } else {
      false
    }
  }

  override suspend fun createMessageChannel(): IMessageChannel {
    val deferred = evalAsyncJavascript<NSArray>(
      "nativeCreateMessageChannel()", null,
      DWebViewWebMessage.webMessagePortContentWorld
    )
    val ports_id = deferred.await()
    val port1_id = ports_id.objectAtIndex(0u) as Int
    val port2_id = ports_id.objectAtIndex(1u) as Int

    val port1 = DWebMessagePort(port1_id, this)
    val port2 = DWebMessagePort(port2_id, this)

    return DWebMessageChannel(port1, port2)
  }

  override suspend fun setContentScale(scale: Float) {
    engine.setContentScaleFactor(scale.toDouble())
  }

  override fun evalAsyncJavascript(code: String): Deferred<String> = engine.evalAsyncJavascript(code)

  fun <T> evalAsyncJavascript(
    code: String,
    wkFrameInfo: WKFrameInfo?,
    wkContentWorld: WKContentWorld
  ): Deferred<T> = engine.evalAsyncJavascript(code, wkFrameInfo, wkContentWorld)

  fun <T> callAsyncJavaScript(
    functionBody: String,
    arguments: Map<Any?, *>?,
    inFrame: WKFrameInfo?,
    inContentWorld: WKContentWorld,
  ) : Deferred<T> = engine.callAsyncJavaScript(functionBody, arguments, inFrame, inContentWorld)
}

