package org.dweb_browser.pure.image.compose

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.ComponentRegistry
import coil3.decode.BitmapFactoryDecoder
import coil3.gif.GifDecoder
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.pure.image.OffscreenWebCanvas

private val contextOffscreenWebCanvasCache = WeakHashMap<Context, OffscreenWebCanvas>()

@Composable
@InternalComposeApi
actual fun rememberOffscreenWebCanvas(): OffscreenWebCanvas {
  val context = LocalContext.current

  return remember(context) {
    var baseContext = context
    while (baseContext is ContextThemeWrapper) {
      baseContext = baseContext.baseContext;
    }
    contextOffscreenWebCanvasCache.getOrPut(baseContext) {
      OffscreenWebCanvas(baseContext)
    }
  }
}

internal actual fun ComponentRegistry.Builder.addPlatformComponents() {
  add(BitmapFactoryDecoder.Factory())
  add(GifDecoder.Factory())
}
