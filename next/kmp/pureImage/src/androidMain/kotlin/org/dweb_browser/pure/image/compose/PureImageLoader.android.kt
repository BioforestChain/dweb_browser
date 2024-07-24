package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import org.dweb_browser.pure.image.offscreenwebcanvas.FetchHook

@Composable
actual fun PureImageLoader.Companion.SmartLoad(
  url: String, maxWidth: Dp, maxHeight: Dp, hook: FetchHook?,
): ImageLoadResult = StableSmartLoad(url, maxWidth, maxHeight, hook)
