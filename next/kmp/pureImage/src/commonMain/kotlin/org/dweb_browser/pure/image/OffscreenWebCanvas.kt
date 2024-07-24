package org.dweb_browser.pure.image

import org.dweb_browser.pure.http.ext.FetchHook
import org.dweb_browser.pure.image.offscreenwebcanvas.OffscreenWebCanvasCore


expect class OffscreenWebCanvas private constructor(width: Int, height: Int) {

  internal val core: OffscreenWebCanvasCore
}


fun OffscreenWebCanvas.setHook(url: String, hook: FetchHook) =
  core.channel.proxy.setHook(url, hook)

