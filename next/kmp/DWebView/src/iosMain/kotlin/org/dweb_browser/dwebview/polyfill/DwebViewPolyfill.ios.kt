package org.dweb_browser.dwebview.polyfill

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SuspendOnce
import org.jetbrains.compose.resources.readResourceBytes

object DwebViewPolyfill {
  internal val prepare = SuspendOnce {
    coroutineScope {
      launch {
        WebSocket = readResourceBytes("dwebview-polyfill/websocket.ios.js").decodeToString()
        Favicon = readResourceBytes("dwebview-polyfill/favicon.ios.js").decodeToString()
      }
    }
  }

  lateinit var WebSocket: String
    private set
  lateinit var Favicon: String
    private set
}