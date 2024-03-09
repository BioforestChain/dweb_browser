package org.dweb_browser.dwebview.polyfill

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SuspendOnce
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.readResourceBytes

object DwebViewPolyfill {
  @OptIn(InternalResourceApi::class)
  internal val prepare = SuspendOnce {
    coroutineScope {
      launch {
        WebSocket = readResourceBytes("files/dwebview-polyfill/websocket.ios.js").decodeToString()
        Favicon = readResourceBytes("files/dwebview-polyfill/favicon.ios.js").decodeToString()
      }
    }
  }

  lateinit var WebSocket: String
    private set
  lateinit var Favicon: String
    private set
}