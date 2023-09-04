package org.dweb_browser.helper.platform

import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.mainAsyncExceptionHandler

actual class PlatformViewController private actual constructor(arg1: Any?, arg2: Any?) {
  constructor() : this(arg1 = null, arg2 = null)

  private var defaultViewWidth = 0
  private var defaultViewHeight = 0
  private var defaultDisplayDensity = 1f
  actual fun getViewWidthPx() =  defaultViewWidth
  actual fun getViewHeightPx() = defaultViewHeight

  actual fun getDisplayDensity() = defaultDisplayDensity

  val context get() = null
  actual val lifecycleScope: CoroutineScope = CoroutineScope(mainAsyncExceptionHandler)


//  private val onResizeSignal by lazy {
//    val signal = Signal<Unit>()
//
//    signal
//  }
//
//
//  actual val onResize = onResizeSignal.toListener()
}