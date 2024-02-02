package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.ComponentRegistry
import coil3.decode.SkiaImageDecoder
import org.dweb_browser.pure.image.OffscreenWebCanvas

private var singletonOffscreenWebCanvasCache: OffscreenWebCanvas? = null

@Composable
internal actual fun rememberOffscreenWebCanvas(): OffscreenWebCanvas {
  return remember {
    if (singletonOffscreenWebCanvasCache == null) {
      singletonOffscreenWebCanvasCache = OffscreenWebCanvas()
    }
    singletonOffscreenWebCanvasCache!!
  }
}

internal actual fun ComponentRegistry.Builder.addPlatformComponents() {
  add(SkiaImageDecoder.Factory())
}