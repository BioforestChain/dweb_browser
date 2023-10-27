//package org.dweb_browser.dwebview
//
//import kotlinx.atomicfu.atomic
//import kotlinx.atomicfu.getAndUpdate
//import kotlinx.coroutines.CancellationException
//import kotlinx.coroutines.Deferred
//
//// 从 commonMain 迁移过来的code start
//import org.dweb_browser.helper.Debugger
//import org.dweb_browser.helper.Signal
//
//val debugDWebView = Debugger("dwebview")
//interface IDWebView {
//  suspend fun loadUrl(url: String, force: Boolean = false): String
//  suspend fun getUrl(): String
//  suspend fun getTitle(): String
//  suspend fun getIcon(): String
//  suspend fun destroy()
//  suspend fun canGoBack(): Boolean
//  suspend fun canGoForward(): Boolean
//  suspend fun goBack(): Boolean
//  suspend fun goForward(): Boolean
//
//  suspend fun createMessageChannel(): IMessageChannel
//
//  suspend fun setContentScale(scale: Float)
//
//  /**
//   * 执行一段JS代码，这个代码将会包裹在 (async()=>{ YOUR_CODE })() 中
//   */
//  fun evalAsyncJavascript(code: String): Deferred<String>
//}
//interface IMessagePort {
//  fun start()
//  fun close()
//  fun postMessage(event: MessageEvent)
//  val onMessage: Signal.Listener<MessageEvent>
//}
//
//interface IMessageChannel {
//  val port1: IMessagePort
//  val port2: IMessagePort
//}
//data class MessageEvent(val data: String, val ports: List<IMessagePort> = emptyList())
//// 从 commonMain 迁移过来的code end
//
//class DWebView(
//  private val engine: DWebViewEngine,
//) : IDWebView {
//
//  private suspend fun loadUrl(task: LoadUrlTask): String {
//    engine.loadUrl(task.url)
//    engine.addNavigationDelegate(DNavigationDelegateProtocol(decidePolicyForNavigationAction = { _, action, _ ->
//      this();
//      task.deferred.complete(
//        action.request.URL?.absoluteString ?: "about:blank"
//      )
//    },
//      didFailNavigation = { _, _, error ->
//        this();
//        task.deferred.completeExceptionally(Exception("[${error.code}] ${error.description}"))
//      }
//    ));
//    task.deferred.invokeOnCompletion {
//      engine.setNavigationDelegate(null)
//    }
//    return task.deferred.await()
//  }
//
//  private val loadUrlTask = atomic<LoadUrlTask?>(null)
//
//  override suspend fun loadUrl(url: String, force: Boolean) = loadUrlTask.getAndUpdate { preTask ->
//    if (!force && preTask?.url == url) {
//      return@getAndUpdate preTask
//    } else {
//      preTask?.deferred?.cancel(CancellationException("load new url: $url"));
//    }
//    val newTask = LoadUrlTask(url)
//    loadUrl(newTask)
//    newTask.deferred.invokeOnCompletion {
//      loadUrlTask.getAndUpdate { preTask ->
//        if (preTask == newTask) null else preTask
//      }
//    }
//    newTask
//  }!!.deferred.await()
//
//  override suspend fun getUrl(): String = loadUrlTask.value?.url ?: "about:blank"
//
//
//  override suspend fun getTitle(): String {
//    return engine.title ?: ""
//  }
//
//  override suspend fun getIcon(): String {
//    TODO("Not yet implemented")
//  }
//
//  override suspend fun destroy() {
//    loadUrl("about:blank", true)
//    engine.removeFromSuperview()
//  }
//
//  override suspend fun canGoBack(): Boolean {
//    return engine.canGoBack
//  }
//
//  override suspend fun canGoForward(): Boolean {
//    return engine.canGoForward
//  }
//
//  override suspend fun goBack(): Boolean {
//    return if (engine.canGoBack) {
//      false
//    } else {
//      engine.goBack()
//      true
//    }
//  }
//
//  override suspend fun goForward(): Boolean {
//    TODO("Not yet implemented")
//  }
//
//  override suspend fun createMessageChannel(): IMessageChannel {
//    TODO("Not yet implemented")
//  }
//
//  override suspend fun setContentScale(scale: Float) {
//    engine.setContentScaleFactor(scale.toDouble())
//  }
//
//  override fun evalAsyncJavascript(code: String): Deferred<String> {
//    TODO("Not yet implemented")
//  }
//}
//
