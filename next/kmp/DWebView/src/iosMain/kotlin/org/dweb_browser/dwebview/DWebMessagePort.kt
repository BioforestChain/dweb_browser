package org.dweb_browser.dwebview

import org.dweb_browser.helper.Signal

class DWebMessagePort(private val engine: DWebViewEngine) : IMessagePort {
  private val onMessageSignal = Signal<MessageEvent>()
  override fun start() {
    TODO("Not yet implemented")
  }

  override fun close() {
    TODO("Not yet implemented")
  }

  override fun postMessage(event: MessageEvent) {
    TODO("Not yet implemented")
  }

  override val onMessage = onMessageSignal.toListener()

}