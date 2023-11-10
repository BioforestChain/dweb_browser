package org.dweb_browser.dwebview

import android.webkit.WebMessagePort

class DWebMessageChannel internal constructor(
  webMessageChannel: Array<WebMessagePort>,
) : IWebMessageChannel {
  override val port1: DWebMessagePort = DWebMessagePort.from(webMessageChannel[0])
  override val port2: DWebMessagePort = DWebMessagePort.from(webMessageChannel[1])
}