package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import coil3.ComponentRegistry
import coil3.decode.SkiaImageDecoder
import org.dweb_browser.pure.image.OffscreenWebCanvas


@Composable
@InternalComposeApi
actual fun rememberOffscreenWebCanvas(): OffscreenWebCanvas {
  return OffscreenWebCanvas.defaultInstance
}

internal actual fun ComponentRegistry.Builder.addPlatformComponents() {
  add(SkiaImageDecoder.Factory())
}