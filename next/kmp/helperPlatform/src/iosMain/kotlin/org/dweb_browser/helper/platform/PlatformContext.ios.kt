package org.dweb_browser.helper.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.helper.mainAsyncExceptionHandler
import platform.UIKit.UIScreen
import platform.UIKit.UIViewController

actual class PlatformViewController private actual constructor(arg1: Any?, arg2: Any?) {
  private val uiViewController: UIViewController = arg1 as UIViewController
  private val uiScreen: UIScreen = arg2 as UIScreen

  constructor(
    uiViewController: UIViewController,
    uiScreen: UIScreen
  ) : this(arg1 = uiViewController, arg2 = uiScreen)

  private var defaultViewWidth = 0
  private var defaultViewHeight = 0
  private var defaultDisplayDensity = 1f

  @OptIn(ExperimentalForeignApi::class)
  actual fun getViewWidthPx() =
    (memScoped { uiViewController.view.frame.ptr[0].size.width.toFloat() } * getDisplayDensity()).toInt()

  @OptIn(ExperimentalForeignApi::class)
  actual fun getViewHeightPx() =
    (memScoped { uiViewController.view.frame.ptr[0].size.height.toFloat() } * getDisplayDensity()).toInt()

  actual fun getDisplayDensity() = uiScreen.scale.toFloat()

  val context get() = null
  actual val lifecycleScope: CoroutineScope = CoroutineScope(mainAsyncExceptionHandler)

}
