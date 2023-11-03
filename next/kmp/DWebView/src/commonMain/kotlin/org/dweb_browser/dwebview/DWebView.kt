package org.dweb_browser.dwebview

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.Signal

val debugDWebView = Debugger("dwebview")

interface IDWebView {
  suspend fun loadUrl(url: String, force: Boolean = false): String
  suspend fun getUrl(): String
  suspend fun getTitle(): String
  suspend fun getIcon(): String
  suspend fun destroy()
  suspend fun canGoBack(): Boolean
  suspend fun canGoForward(): Boolean
  suspend fun goBack(): Boolean
  suspend fun goForward(): Boolean

  suspend fun createMessageChannel(): IMessageChannel

  suspend fun setContentScale(scale: Float)
}

interface IMessageChannel {
  val port1: IWebMessagePort
  val port2: IWebMessagePort
}

interface IWebMessagePort {
  suspend fun start()
  suspend fun close()
  suspend fun postMessage(event: IMessageEvent)
  val onMessage: Signal.Listener<IMessageEvent>
}

internal class LoadUrlTask(
  val url: String,
  val deferred: CompletableDeferred<String> = CompletableDeferred()
)

interface IMessageEvent {
  val data: String
  val ports: List<IWebMessagePort>
}

typealias AsyncChannel = Channel<Result<String>>