package org.dweb_browser.helper.platform

import kotlinx.coroutines.CoroutineScope

interface IPlatformViewController {
  val lifecycleScope: CoroutineScope
  suspend fun getViewWidthPx(): Int;
  suspend fun getViewHeightPx(): Int;
  suspend fun getDisplayDensity(): Float;

//  val windowInsetsCompat: WindowInsets
//
//  val onResize: Signal.Listener<Unit>
}
//expect interface PlatformContext {
//  val coroutineScope: CoroutineScope
//}

