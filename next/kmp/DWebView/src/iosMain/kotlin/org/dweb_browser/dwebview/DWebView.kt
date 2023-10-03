package org.dweb_browser.dwebview

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred

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
      false
    } else {
      engine.goBack()
      true
    }
  }

  override suspend fun goForward(): Boolean {
    TODO("Not yet implemented")
  }

  override suspend fun createMessageChannel(): IMessageChannel {

  }

  override suspend fun setContentScale(scale: Float) {
    engine.setContentScaleFactor(scale.toDouble())
  }

  override fun evalAsyncJavascript(code: String): Deferred<String> {
    TODO("Not yet implemented")
  }
}

