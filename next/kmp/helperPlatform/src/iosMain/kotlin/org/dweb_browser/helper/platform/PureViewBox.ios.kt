package org.dweb_browser.helper.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.mainAsyncExceptionHandler
import platform.UIKit.UIScreen
import platform.UIKit.UIViewController

actual fun IPureViewBox.Companion.from(viewController: IPureViewController): IPureViewBox {
  require(viewController is PureViewController)
  return PureViewBox.instances.getOrPut(viewController.uiViewController) {
    PureViewBox(
      viewController.uiViewController
    )
  }
}

class PureViewBox(
  uiViewController: UIViewController? = null,
  private val uiScreen: UIScreen = UIScreen.mainScreen
) : IPureViewBox {
  companion object {
    internal val instances = WeakHashMap<UIViewController, IPureViewBox>()
  }

  private var defaultViewWidth = 0
  private var defaultViewHeight = 0
  private var defaultDisplayDensity = 1f
  private val uiViewControllerDeferred = CompletableDeferred<UIViewController>()
  fun setUiViewController(uiViewController: UIViewController): Boolean {
    if (uiViewControllerDeferred.isCompleted) return false
    uiViewControllerDeferred.complete(uiViewController)
    return true
  }

  suspend fun hasUiViewController() = uiViewControllerDeferred.isCompleted
  suspend fun uiViewController() = uiViewControllerDeferred.await()

  init {
    if (uiViewController != null) {
      setUiViewController(uiViewController)
    }
  }


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