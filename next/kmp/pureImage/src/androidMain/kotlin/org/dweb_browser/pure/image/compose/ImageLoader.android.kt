package org.dweb_browser.pure.image.compose

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.pure.image.OffscreenWebCanvas

private val contextOffscreenWebCanvasCache = WeakHashMap<Context, OffscreenWebCanvas>()

@Composable
internal actual fun rememberOffscreenWebCanvas(): OffscreenWebCanvas {
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