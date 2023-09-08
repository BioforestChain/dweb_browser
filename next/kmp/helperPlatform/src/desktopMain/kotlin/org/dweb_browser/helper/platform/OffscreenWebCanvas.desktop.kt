package org.dweb_browser.helper.platform

import org.dweb_browser.helper.platform.offscreenwebcanvas.OffscreenWebCanvasCore

actual class OffscreenWebCanvas actual constructor(width: Int, height: Int) {
  internal actual val core = OffscreenWebCanvasCore()
}