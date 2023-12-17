package org.dweb_browser.dwebview.polyfill

import kotlinx.coroutines.runBlocking
import org.dweb_browser.helper.toUtf8
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource

object WebSocketProxy {
  @OptIn(ExperimentalResourceApi::class)
  private val WebSocketPolyfillScript by lazy {
    runBlocking { resource("dwebview-polyfill/websocket.ios.js").readBytes().toUtf8() }
  }

  fun getPolyfillScript() = WebSocketPolyfillScript
}