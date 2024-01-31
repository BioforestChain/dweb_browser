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
        KeyBoard = readResourceBytes("dwebview-polyfill/keyboard.android.js").decodeToString()
      }
    }
  }

  lateinit var KeyBoard: String
    private set
}