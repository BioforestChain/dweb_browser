package org.dweb_browser.pure.image

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.envSwitch
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.platform.desktop.os.dataDir
import org.dweb_browser.platform.desktop.webview.WebviewEngine
import org.dweb_browser.pure.image.offscreenwebcanvas.OffscreenWebCanvasCore
import org.jetbrains.annotations.ApiStatus.Internal

actual class OffscreenWebCanvas private actual constructor(width: Int, height: Int) {
  companion object {
    val defaultInstance by lazy { OffscreenWebCanvas() }
  }

  internal actual val core = OffscreenWebCanvasCore()

  @Internal
  val webview =
    WebviewEngine.offScreen(dataDir = dataDir.resolve(("offscreen-web-canvas"))).newBrowser().also {
      if (envSwitch.isEnabled("offscreen-web-canvas-devtools")) {
        it.devTools().show()
      }
    }

  constructor() : this(128, 128)

  init {
    CoroutineScope(mainAsyncExceptionHandler).launch {
      webview.navigation().loadUrl(core.channel.getEntryUrl(width, height))
    }
  }

}