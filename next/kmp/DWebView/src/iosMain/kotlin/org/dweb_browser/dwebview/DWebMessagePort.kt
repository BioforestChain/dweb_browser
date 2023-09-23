package org.dweb_browser.dwebview

import org.dweb_browser.helper.Signal

class DWebMessagePort() : IMessagePort {
    private val onMessageSignal = Signal<String>()
    override val onMessage = onMessageSignal.toListener()
    override fun postMessage(data: String) {

    }
}