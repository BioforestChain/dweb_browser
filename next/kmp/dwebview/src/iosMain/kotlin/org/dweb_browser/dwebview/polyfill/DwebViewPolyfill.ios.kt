package org.dweb_browser.dwebview.polyfill

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SuspendOnce

object DwebViewIosPolyfill : DwebViewCommonPolyfill() {
  internal val prepare = SuspendOnce {
    coroutineScope {
      launch { WebSocket = readDwebviewPolyfill("websocket.ios.js") }
      launch { Favicon = readDwebviewPolyfill("favicon.ios.js") }
      launch { CloseWatcher = readDwebviewPolyfill("close-watcher.common.js") }
      launch { UserAgentData = readDwebviewPolyfill("user-agent-data.common.js") }
      launch { NavigationHook = readDwebviewPolyfill("navigation-hook.ios.js") }
      launch { WebMessage = readDwebviewPolyfill("web-message.ios.js") }
    }
  }

  lateinit var WebSocket: String
    private set
  lateinit var Favicon: String
    private set
  lateinit var CloseWatcher: String
    private set
  lateinit var UserAgentData: String
    private set
  lateinit var NavigationHook: String
    private set
  lateinit var WebMessage: String
    private set
}