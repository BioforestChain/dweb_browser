package org.dweb_browser.dwebview

import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.Signal

val debugDWebView = Debugger("dwebview")

interface IDWebView {
  fun loadUrl(url: String)
  fun destroy()
  fun canGoBack(): Boolean
  fun canGoForward(): Boolean

  fun createMessageChannel(): IMessageChannel

  fun setContentScale(scale: Float)

  suspend fun evalJavascriptAsync(code: String): String
}

interface IMessageChannel {
  val port1: IMessagePort
  val port2: IMessagePort
}

interface IMessagePort {
  fun postMessage(data: String)
  val onMessage: Signal.Listener<String>
}