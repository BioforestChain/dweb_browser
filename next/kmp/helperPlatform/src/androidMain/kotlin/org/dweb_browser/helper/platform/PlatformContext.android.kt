package org.dweb_browser.helper.platform

import android.content.Context
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.android.BaseActivity
import org.dweb_browser.helper.android.addOnApplyWindowInsetsCompatListener
import org.dweb_browser.helper.mainAsyncExceptionHandler

actual class PlatformViewController private actual constructor(arg1: Any?, arg2: Any?) {
  companion object {
    var appContext: Context? = null
  }

  private val baseActivity: BaseActivity? = arg2 as BaseActivity?
  private val context: Context = arg1 as Context

  constructor(androidContext: Context) : this(
    arg1 = androidContext, when (val activity = androidContext) {
      is BaseActivity -> activity
      else -> null
    }
  )

  private var defaultViewWidth = 0
  private var defaultViewHeight = 0
  private var defaultDisplayDensity = 1f
  actual fun getViewWidthPx() = baseActivity?.let { it.window.decorView.width } ?: defaultViewWidth
  actual fun getViewHeightPx() =
    baseActivity?.let { it.window.decorView.height } ?: defaultViewHeight

  actual fun getDisplayDensity() =
    baseActivity?.let { it.resources.displayMetrics.density } ?: defaultDisplayDensity

  val androidContext get() = context
  val activity get() = baseActivity
  actual val lifecycleScope: CoroutineScope = baseActivity?.lifecycleScope ?: CoroutineScope(
    mainAsyncExceptionHandler
  )

//  private var defaultWindowInsetsCompat = baseActivity?.let {
//    WindowInsetsCompat.toWindowInsetsCompat(
//      it.window.decorView.rootWindowInsets
//    )
//  } ?: WindowInsetsCompat.CONSUMED
//
//  val windowInsetsCompat get() = defaultWindowInsetsCompat
//
//  private val onResizeSignal by lazy {
//    val signal = Signal<Unit>()
//    baseActivity?.also {
//      it.window.decorView.addOnApplyWindowInsetsCompatListener { _, insets ->
//        defaultWindowInsetsCompat = insets
//        coroutineScope.launch {
//          signal.emit(Unit)
//        }
//        insets
//      }
//    }
//    signal
//  }
//
//
//  actual val onResize = onResizeSignal.toListener()
}