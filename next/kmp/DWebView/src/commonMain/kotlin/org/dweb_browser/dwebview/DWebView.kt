//package org.dweb_browser.dwebview
//
//import kotlinx.coroutines.CompletableDeferred
//import kotlinx.coroutines.Deferred
//import org.dweb_browser.helper.Debugger
//import org.dweb_browser.helper.Signal
//
//val debugDWebView = Debugger("dwebview")
//
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
//
//interface IMessageChannel {
//  val port1: IMessagePort
//  val port2: IMessagePort
//}
//
//interface IMessagePort {
//  fun start()
//  fun close()
//  fun postMessage(event: MessageEvent)
//  val onMessage: Signal.Listener<MessageEvent>
//}
//
//internal class LoadUrlTask(
//  val url: String,
//  val deferred: CompletableDeferred<String> = CompletableDeferred()
//)
//
//data class MessageEvent(val data: String, val ports: List<IMessagePort> = emptyList())