package org.dweb_browser.dwebview.polyfill

import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.readResourceBytes

open class DwebViewCommonPolyfill {
  @OptIn(InternalResourceApi::class)
  suspend fun readDwebviewPolyfill(filename: String) =
    readResourceBytes("files/dwebview-polyfill/$filename").decodeToString()
}