package org.dweb_browser.dwebview.polyfill

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SuspendOnce
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource

object DwebViewPolyfill {
  @OptIn(ExperimentalResourceApi::class)
  internal val prepare = SuspendOnce {
    coroutineScope {
      launch {
        WebSocket = resource("dwebview-polyfill/websocket.ios.js").readBytes().decodeToString()
        Favicon = resource("dwebview-polyfill/favicon.ios.js").readBytes().decodeToString()
      }
    }
  }

  lateinit var WebSocket: String
    private set
  lateinit var Favicon: String
    private set
}