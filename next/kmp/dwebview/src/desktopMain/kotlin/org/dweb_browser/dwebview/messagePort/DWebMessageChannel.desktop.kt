package org.dweb_browser.dwebview.messagePort

import com.teamdev.jxbrowser.js.JsObject
import org.dweb_browser.core.ipc.helper.IWebMessageChannel

class DWebMessageChannel internal constructor(
  override val port1: DWebMessagePort, override val port2: DWebMessagePort,
) : IWebMessageChannel {

}