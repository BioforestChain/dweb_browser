package org.dweb_browser.helper.compose

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.platform.OffscreenWebCanvas

private val contextOffscreenWebCanvasCache = WeakHashMap<Context, OffscreenWebCanvas>()

@Composable
internal actual fun rememberOffscreenWebCanvas(): OffscreenWebCanvas {
  val context = LocalContext.current
  return remember(context) {
    contextOffscreenWebCanvasCache.getOrPut(context) {
      OffscreenWebCanvas(context)
    }
  }
}