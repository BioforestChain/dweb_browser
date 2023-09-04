package org.dweb_browser.helper.platform

import kotlinx.coroutines.CoroutineScope

expect class PlatformViewController private constructor(arg1: Any?, arg2: Any?) {
  val lifecycleScope: CoroutineScope
  fun getViewWidthPx(): Int;
  fun getViewHeightPx(): Int;
  fun getDisplayDensity(): Float;

//  val windowInsetsCompat: WindowInsets
//
//  val onResize: Signal.Listener<Unit>
}
//expect interface PlatformContext {
//  val coroutineScope: CoroutineScope
//}

