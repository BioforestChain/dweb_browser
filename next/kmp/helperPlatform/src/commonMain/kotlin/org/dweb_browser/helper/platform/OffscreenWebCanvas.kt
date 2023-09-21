package org.dweb_browser.helper.platform

import org.dweb_browser.helper.platform.offscreenwebcanvas.FetchHook
import org.dweb_browser.helper.platform.offscreenwebcanvas.OffscreenWebCanvasCore


expect class OffscreenWebCanvas private constructor(width: Int, height: Int) {

  internal val core: OffscreenWebCanvasCore
}


fun OffscreenWebCanvas.setHook(url: String, hook: FetchHook) =
  core.channel.proxy.setHook(url, hook)

