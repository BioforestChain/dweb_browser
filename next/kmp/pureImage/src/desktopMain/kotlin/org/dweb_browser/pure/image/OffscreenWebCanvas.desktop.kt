package org.dweb_browser.pure.image

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.platform.desktop.webview.WebviewEngine
import org.dweb_browser.pure.image.offscreenwebcanvas.OffscreenWebCanvasCore

actual class OffscreenWebCanvas private actual constructor(width: Int, height: Int) {
  internal actual val core = OffscreenWebCanvasCore()
  private val webview = WebviewEngine.offScreen.newBrowser()

  constructor() : this(128, 128)

  init {
    CoroutineScope(mainAsyncExceptionHandler).launch {
      webview.navigation().loadUrl(core.channel.getEntryUrl(width, height))
    }
  }

}