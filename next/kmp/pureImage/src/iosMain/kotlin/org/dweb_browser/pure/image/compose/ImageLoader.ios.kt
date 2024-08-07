package org.dweb_browser.pure.image.compose

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import coil3.ComponentRegistry
import coil3.decode.SkiaImageDecoder
import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.pure.image.OffscreenWebCanvas


@OptIn(ExperimentalForeignApi::class)
@Composable
@InternalComposeApi
actual fun rememberOffscreenWebCanvas(): OffscreenWebCanvas {
  return OffscreenWebCanvas.defaultInstance.collectAsState().value
}

internal actual fun ComponentRegistry.Builder.addPlatformComponents() {
  add(SkiaImageDecoder.Factory())
}