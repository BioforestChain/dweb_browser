package org.dweb_browser.dwebview.messagePort

import org.dweb_browser.core.ipc.helper.IWebMessageChannel

class DWebMessageChannel internal constructor(
  override val port1: DWebMessagePort, override val port2: DWebMessagePort
) : IWebMessageChannel {}