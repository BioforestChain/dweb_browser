package org.dweb_browser.dwebview.polyfill

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SuspendOnce

object DwebViewDesktopPolyfill : DwebViewCommonPolyfill() {
  internal val prepare = SuspendOnce {
    coroutineScope {
      launch {
        Favicon = readDwebviewPolyfill("favicon.common.js")
        CloseWatcher = readDwebviewPolyfill("close-watcher.common.js")
        UserAgentData = readDwebviewPolyfill("user-agent-data.common.js")
        WebMessagePort = readDwebviewPolyfill("web-message-port.desktop.js")
      }
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