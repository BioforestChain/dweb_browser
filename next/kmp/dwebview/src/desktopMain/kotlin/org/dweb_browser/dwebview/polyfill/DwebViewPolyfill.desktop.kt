package org.dweb_browser.dwebview.polyfill

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SuspendOnce

object DwebViewDesktopPolyfill : DwebViewCommonPolyfill() {
  internal val prepare = SuspendOnce {
    coroutineScope {
      launch { Favicon = readDwebviewPolyfill("favicon.common.js") }
      launch { CloseWatcher = readDwebviewPolyfill("close-watcher.common.js") }
      launch { UserAgentData = readDwebviewPolyfill("user-agent-data.common.js") }
      launch { WebMessagePort = readDwebviewPolyfill("web-message-port.desktop.js") }
    }
  }

  lateinit var Favicon: String
    private set
  lateinit var CloseWatcher: String
    private set
  lateinit var UserAgentData: String
    private set
  lateinit var WebMessagePort: String
    private set
}