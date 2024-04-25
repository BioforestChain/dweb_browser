package org.dweb_browser.dwebview.polyfill

import dweb_browser_kmp.dwebview.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

open class DwebViewCommonPolyfill {
  @OptIn(ExperimentalResourceApi::class)
  suspend fun readDwebviewPolyfill(filename: String) =
    Res.readBytes("files/dwebview-polyfill/$filename").decodeToString()
}