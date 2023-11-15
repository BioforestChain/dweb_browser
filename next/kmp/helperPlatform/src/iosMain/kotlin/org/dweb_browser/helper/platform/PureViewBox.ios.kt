package org.dweb_browser.helper.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.helper.mainAsyncExceptionHandler
import platform.UIKit.UIScreen
import platform.UIKit.UIViewController

class PureViewBox(
  private val uiScreen: UIScreen
) : IPureViewBox {
  private var defaultViewWidth = 0
  private var defaultViewHeight = 0
  private var defaultDisplayDensity = 1f
  private val uiViewControllerDeferred = CompletableDeferred<UIViewController>()
  fun setUiViewController(uiViewController: UIViewController): Boolean {
    if (uiViewControllerDeferred.isCompleted) return false
    uiViewControllerDeferred.complete(uiViewController)
    return true
  }

  suspend fun uiViewController() = uiViewControllerDeferred.await()


  @OptIn(ExperimentalForeignApi::class)
  override suspend fun getViewWidthPx() =
    (memScoped { uiViewController().view.frame.ptr[0].size.width.toFloat() } * getDisplayDensity()).toInt()

  @OptIn(ExperimentalForeignApi::class)
  override suspend fun getViewHeightPx() =
    (memScoped { uiViewController().view.frame.ptr[0].size.height.toFloat() } * getDisplayDensity()).toInt()

  override suspend fun getDisplayDensity() = uiScreen.scale.toFloat()

  override val lifecycleScope: CoroutineScope = CoroutineScope(mainAsyncExceptionHandler)
}

fun IPureViewBox.asIos(): PureViewBox {
  require(this is PureViewBox)
  return this
}