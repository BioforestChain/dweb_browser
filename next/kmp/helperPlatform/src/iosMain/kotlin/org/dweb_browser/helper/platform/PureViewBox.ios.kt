package org.dweb_browser.helper.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.helper.withMainContext
import platform.UIKit.UIScreen
import platform.UIKit.UIViewController

actual suspend fun IPureViewBox.Companion.from(viewController: IPureViewController): IPureViewBox {
  require(viewController is PureViewController)
  return PureViewBox.instances.getOrPut(viewController) {
    val vc = viewController.getUiViewController()
    viewController.waitInit() /// 等待附加到UIWindow中
    PureViewBox(vc)
  }
}

class PureViewBox(
  private val uiViewController: UIViewController,
  private val uiScreen: UIScreen = UIScreen.mainScreen
) : IPureViewBox {
  companion object {
    internal val instances = WeakHashMap<IPureViewController, IPureViewBox>()
  }

  private var defaultViewWidth = 0
  private var defaultViewHeight = 0
  private var defaultDisplayDensity = 1f

  @OptIn(ExperimentalForeignApi::class)
  override suspend fun getViewWidthPx() = runBlockingCatching {
    (withMainContext { memScoped { uiViewController.view.frame.ptr[0].size.width.toFloat() } } * getDisplayDensity()).toInt()
  }.getOrDefault(defaultViewWidth)

  @OptIn(ExperimentalForeignApi::class)
  override suspend fun getViewHeightPx() = runBlockingCatching {
    (withMainContext { memScoped { uiViewController.view.frame.ptr[0].size.height.toFloat() } } * getDisplayDensity()).toInt()
  }.getOrDefault(defaultViewHeight)

  override suspend fun getDisplayDensity() =
    runBlockingCatching { withMainContext { uiScreen.scale.toFloat() } }.getOrDefault(
      defaultDisplayDensity
    )

  override val lifecycleScope: CoroutineScope =
    CoroutineScope(mainAsyncExceptionHandler + SupervisorJob())
}

fun IPureViewBox.asIos(): PureViewBox {
  require(this is PureViewBox)
  return this
}