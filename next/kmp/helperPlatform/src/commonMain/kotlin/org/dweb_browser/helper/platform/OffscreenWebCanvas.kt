package org.dweb_browser.helper.platform

import org.dweb_browser.helper.platform.offscreenwebcanvas.OffscreenWebCanvasCore


expect class OffscreenWebCanvas private constructor(width: Int, height: Int) {
  internal val core: OffscreenWebCanvasCore
}


