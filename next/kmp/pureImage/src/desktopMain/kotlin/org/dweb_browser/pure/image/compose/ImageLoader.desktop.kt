package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.remember
import coil3.ComponentRegistry
import coil3.decode.SkiaImageDecoder
import org.dweb_browser.pure.image.OffscreenWebCanvas

@Composable
@InternalComposeApi
actual fun rememberOffscreenWebCanvas(): OffscreenWebCanvas {
  return remember {
    OffscreenWebCanvas.defaultInstance
  }
}

internal actual fun ComponentRegistry.Builder.addPlatformComponents() {
  add(SkiaImageDecoder.Factory())
}