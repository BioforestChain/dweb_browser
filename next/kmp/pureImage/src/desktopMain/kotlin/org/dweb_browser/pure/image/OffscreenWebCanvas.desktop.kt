package org.dweb_browser.pure.image

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.platform.getOrCreateProfile
import org.dweb_browser.helper.platform.webViewEngine
import org.dweb_browser.pure.image.offscreenwebcanvas.OffscreenWebCanvasCore
import org.jetbrains.annotations.ApiStatus.Internal

actual class OffscreenWebCanvas private actual constructor(width: Int, height: Int) {
  companion object {
    val defaultInstance by lazy { OffscreenWebCanvas() }
  }

  internal actual val core = OffscreenWebCanvasCore()

  // TODO 将 OffscreenWebCanvas 做成一个模块，这样在 数据管理 中，以“系统缓存”的身份出现，会更好管理一些
  @Internal
  val webview = webViewEngine.offScreenEngine.profiles().getOrCreateProfile("offscreen-web-canvas")
    .newBrowser().also {
      if (envSwitch.isEnabled("offscreen-web-canvas-devtools")) {
        it.devTools().show()
      }
    }

  constructor() : this(128, 128)

  init {
    CoroutineScope(mainAsyncExceptionHandler).launch {
      core.channel.entryUrlFlow.collect {
        webview.navigation().loadUrl(it)
      }
    }
  }

}